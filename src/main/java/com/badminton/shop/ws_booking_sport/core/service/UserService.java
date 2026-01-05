package com.badminton.shop.ws_booking_sport.core.service;

import com.badminton.shop.ws_booking_sport.handleException.AuthenticationFailedException;
import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.core.repository.UserRepository;
import com.badminton.shop.ws_booking_sport.dto.request.*;
import com.badminton.shop.ws_booking_sport.dto.response.*;
import com.badminton.shop.ws_booking_sport.enums.AuthProvider;
import com.badminton.shop.ws_booking_sport.enums.Role;
import com.badminton.shop.ws_booking_sport.goong.GoongMapService;
import com.badminton.shop.ws_booking_sport.goong.GoongResponse;
import com.badminton.shop.ws_booking_sport.model.core.*;
import com.badminton.shop.ws_booking_sport.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final VerificationRateLimiter verificationRateLimiter;
    private final GoongMapService goongMapService;
    private static final int VERIFY_CODE_EXPIRY_MINUTES = 15; // verify code valid for 15 minutes

    // Lấy ClientID từ file properties
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    private String generateVerificationCode() {
        Random rnd = new Random();
        int number = 100000 + rnd.nextInt(900000);
        return String.valueOf(number);
    }
    public AuthResponse authenticateGoogle(String idTokenString) {
        try {
            // 1. Cấu hình Verifier
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // 2. Verify Token
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new AuthenticationFailedException("Invalid or expired Google ID token");
            }

            // 3. Lấy thông tin từ Google Payload
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String fullName = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            // 4. Tìm hoặc Tạo Account mới
            Account account = accountRepository.findByEmail(email).orElseGet(() -> {
                Account newAccount = new Account();
                User newUser = new User();
                newUser.setName(fullName);
                newUser.setActive(true);
                newUser.setAvatarUrl(picture);
                newUser.setCreatedAt(LocalDateTime.now());
                newUser = userRepository.save(newUser);
                newAccount.setUser(newUser);
                newAccount.setEmail(email);
                newAccount.setPassword(null); // Không cần pass
                newAccount.setAuthProvider(AuthProvider.GOOGLE);

                // Set Role mặc định (Nên xử lý kỹ hơn ở đây thay vì try-catch lỏng lẻo)
                try {
                    newAccount.setRole(Role.CUSTOMER);
                } catch (Exception e) {
                    // Log warning nếu cần
                }
                return accountRepository.save(newAccount);
            });

            // Ensure account role for social logins is CUSTOMER (force for existing accounts)
            if (account.getRole() == null || account.getRole() != Role.CUSTOMER) {
                account.setRole(Role.CUSTOMER);
                accountRepository.save(account);
            }

            // 5. Tạo JWT Token
            String accessToken = jwtService.generateAccessToken(account.getUser(), email, account.getRole());
            String refreshToken = jwtService.generateRefreshToken(account.getUser(), email, account.getRole());

            // 6. Trả về Response
            Integer venueId = null;
            if (account.getUser() instanceof com.badminton.shop.ws_booking_sport.model.core.Owner) {
                com.badminton.shop.ws_booking_sport.model.core.Owner owner = (com.badminton.shop.ws_booking_sport.model.core.Owner) account.getUser();
                if (owner.getVenues() != null && !owner.getVenues().isEmpty() && owner.getVenues().get(0) != null) {
                    venueId = owner.getVenues().get(0).getId();
                }
            }

            return new AuthResponse(
                    account.getUser().getId(),
                    account.getUser().getName(),
                    account.getEmail(),
                    accessToken,
                    refreshToken,
                    account.getRole(),
                    venueId
            );
        } catch (GeneralSecurityException | IOException ex) {
            throw new AuthenticationFailedException("Failed to verify Google ID token", ex);
        }
    }

    // Facebook login: validate access token via Facebook Graph API and create/find account
    @SuppressWarnings({"unchecked", "rawtypes"})
    public AuthResponse authenticateFacebook(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) throw new AuthenticationFailedException("Access token is required");

        RestTemplate rest = new RestTemplate();
        String encoded = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String url = "https://graph.facebook.com/me?fields=id,name,email,picture&access_token=" + encoded;

        Map<String, Object> fbResp;
        try {
            fbResp = rest.getForObject(url, Map.class);
        } catch (Exception ex) {
            throw new AuthenticationFailedException("Invalid Facebook access token", ex);
        }

        if (fbResp == null || fbResp.get("id") == null) {
            throw new AuthenticationFailedException("Invalid Facebook access token");
        }

        String fbId = String.valueOf(fbResp.get("id"));
        String email = (String) fbResp.get("email");
        String fullName = (String) fbResp.get("name");
        String picture = null;
        try {
            Object picObj = fbResp.get("picture");
            if (picObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> picMap = (Map<String, Object>) picObj;
                Object data = picMap.get("data");
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    Object urlObj = dataMap.get("url");
                    if (urlObj != null) picture = urlObj.toString();
                }
            }
        } catch (Exception ignored) {
        }

        // If Facebook doesn't expose email (user didn't grant), synthesize one to ensure uniqueness
        if (email == null || email.isBlank()) {
            email = fbId + "@facebook.local";
        }

        // make final copies for lambda capture
        final String finalEmail = email;
        final String finalFullName = fullName != null ? fullName : "Facebook User";
        final String finalPicture = picture;

        Account account = accountRepository.findByEmail(finalEmail).orElseGet(() -> {
            Account newAccount = new Account();
            User newUser = new User();
            newUser.setName(finalFullName);
            newUser.setActive(true);
            newUser.setAvatarUrl(finalPicture);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser = userRepository.save(newUser);

            newAccount.setUser(newUser);
            newAccount.setEmail(finalEmail);
            newAccount.setPassword(null);
            newAccount.setAuthProvider(AuthProvider.FACEBOOK);
            try {
                newAccount.setRole(Role.CUSTOMER);
            } catch (Exception e) {
                // ignore
            }
            return accountRepository.save(newAccount);
        });

        // Ensure account role for social logins is CUSTOMER (force for existing accounts)
        if (account.getRole() == null || account.getRole() != Role.CUSTOMER) {
            account.setRole(Role.CUSTOMER);
            accountRepository.save(account);
        }

        // generate tokens
        String access = jwtService.generateAccessToken(account.getUser(), account.getEmail(), account.getRole());
        String refresh = jwtService.generateRefreshToken(account.getUser(), account.getEmail(), account.getRole());

        Integer venueId = null;
        if (account.getUser() instanceof com.badminton.shop.ws_booking_sport.model.core.Owner) {
            com.badminton.shop.ws_booking_sport.model.core.Owner owner = (com.badminton.shop.ws_booking_sport.model.core.Owner) account.getUser();
            if (owner.getVenues() != null && !owner.getVenues().isEmpty() && owner.getVenues().get(0) != null) {
                venueId = owner.getVenues().get(0).getId();
            }
        }

        return new AuthResponse(account.getUser().getId(), account.getUser().getName(), account.getEmail(), access, refresh, account.getRole(), venueId);
    }


    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        // validate password match
        if (req.getPassword() == null || !req.getPassword().equals(req.getVerifyPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String email = req.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (accountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // create a Customer (Customer extends User) as default role
        Customer user = new Customer();
        user.setName(req.getName());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        user = userRepository.save(user);

        // assign default role CUSTOMER
        Role role = Role.CUSTOMER;

        // create Account
        Account account = new Account();
        account.setEmail(email);
        account.setAuthProvider(AuthProvider.LOCAL);
        account.setPassword(passwordEncoder.encode(req.getPassword()));
        account.setVerified(false);
        account.setLastLogin(null);
        String verifyCode = generateVerificationCode();
        account.setVerifyCode(verifyCode);
        account.setVerifyCodeExpiry(LocalDateTime.now().plusMinutes(VERIFY_CODE_EXPIRY_MINUTES));
        account.setRole(role);
        account.setUser(user);

        accountRepository.save(account);

        // send verification email (best-effort)
        try {
            emailService.sendVerificationCode(email, verifyCode);
        } catch (Exception ex) {
            // Log the exception in production; do not interrupt registration for transient mail errors
        }

        // generate JWT access and refresh tokens including role (but user cannot use them until verified)
        String accessToken = jwtService.generateAccessToken(user, email, role);
        String refreshToken = jwtService.generateRefreshToken(user, email, role);

        return new RegisterResponse(user.getId(), user.getName(), email, accessToken, refreshToken, role);
    }

    public void resendVerification(ResendVerifyRequest req) {
        String email = req.getEmail();
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");

        // rate limit resend attempts
        if (!verificationRateLimiter.allow(email)) {
            throw new IllegalArgumentException("Too many verification attempts. Please try again later.");
        }

        Account account = accountRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (account.isVerified()) throw new IllegalArgumentException("Account already verified");

        String code = generateVerificationCode();
        account.setVerifyCode(code);
        account.setVerifyCodeExpiry(LocalDateTime.now().plusMinutes(VERIFY_CODE_EXPIRY_MINUTES));
        accountRepository.save(account);

        emailService.sendVerificationCode(email, code);
    }

    public String confirmVerification(VerifyRequest req) {
        String email = req.getEmail();
        String code = req.getCode();
        if (email == null || email.isBlank() || code == null || code.isBlank()) throw new IllegalArgumentException("Email and code are required");

        Account account = accountRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (account.isVerified()) return "Already verified";

        LocalDateTime expiry = account.getVerifyCodeExpiry();
        if (expiry == null || expiry.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code expired. Please request a new code.");
        }

        if (code.equals(account.getVerifyCode())) {
            account.setVerified(true);
            account.setVerifyCode(null);
            account.setVerifyCodeExpiry(null);
            accountRepository.save(account);
            return "Verified";
        } else {
            throw new IllegalArgumentException("Invalid verification code");
        }
    }

    public RefreshResponse refresh(RefreshRequest req) {
        String refreshToken = req.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        // extract userId and email from token
        String subject = jwtService.extractSubject(refreshToken);
        String email = jwtService.extractEmail(refreshToken);
        Role role = jwtService.extractRole(refreshToken);

        if (subject == null || email == null || role == null) {
            throw new IllegalArgumentException("Invalid refresh token claims");
        }

        int userId;
        try {
            userId = Integer.parseInt(subject);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user id in token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Optionally verify account exists and role matches
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getRole() != role) {
            throw new IllegalArgumentException("Token role does not match account role");
        }

        // generate new tokens (rotate refresh token)
        String newAccess = jwtService.generateAccessToken(user, email, role);
        String newRefresh = jwtService.generateRefreshToken(user, email, role);

        return new RefreshResponse(newAccess, newRefresh, role);
    }

    public AuthResponse login(AuthRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        Account account = accountRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!account.isVerified()) {
            throw new IllegalArgumentException("Account not verified. Please verify your email before logging in.");
        }

        if (!passwordEncoder.matches(req.getPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        User user = account.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Associated user not found");
        }

        account.setLastLogin(LocalDateTime.now());
        accountRepository.save(account);

        Role role = account.getRole();
        String access = jwtService.generateAccessToken(user, account.getEmail(), role);
        String refresh = jwtService.generateRefreshToken(user, account.getEmail(), role);

        Integer venueId = null;
        if (user instanceof com.badminton.shop.ws_booking_sport.model.core.Owner) {
            com.badminton.shop.ws_booking_sport.model.core.Owner owner = (com.badminton.shop.ws_booking_sport.model.core.Owner) user;
            if (owner.getVenues() != null && !owner.getVenues().isEmpty() && owner.getVenues().get(0) != null) {
                venueId = owner.getVenues().get(0).getId();
            }
        }

        return new AuthResponse(user.getId(), user.getName(), account.getEmail(), access, refresh, role, venueId);
    }

    public RegisterResponse registerOwner(RegisterRequest req) {
        // validate password match
        if (req.getPassword() == null || !req.getPassword().equals(req.getVerifyPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String email = req.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (accountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // create an Owner (Owner extends User)
        Owner user = new Owner();
        user.setName(req.getName());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setRating(0.0f);

        user = userRepository.save(user);

        // assign role OWNER
        Role role = Role.OWNER;

        // create Account for owner
        Account account = new Account();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(req.getPassword()));
        account.setVerified(false);
        account.setLastLogin(null);
        String verifyCode = generateVerificationCode();
        account.setVerifyCode(verifyCode);
        account.setVerifyCodeExpiry(LocalDateTime.now().plusMinutes(VERIFY_CODE_EXPIRY_MINUTES));
        account.setRole(role);
        account.setUser(user);

        accountRepository.save(account);

        // send verification email (best-effort)
        try {
            emailService.sendVerificationCode(email, verifyCode);
        } catch (Exception ex) {
            // Log the exception in production; do not interrupt registration for transient mail errors
        }

        // generate JWT access and refresh tokens including role (but owner cannot use them until verified)
        String accessToken = jwtService.generateAccessToken(user, email, role);
        String refreshToken = jwtService.generateRefreshToken(user, email, role);

        return new RegisterResponse(user.getId(), user.getName(), email, accessToken, refreshToken, role);
    }

    // new: logout method - accepts an Authorization header (Bearer token) and sets account.logoutAt
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token does not contain email");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setLogoutAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    // new: return current authenticated user's details
    public MeResponse me(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token does not contain email");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // check logout timestamp
        Date issued = jwtService.extractIssuedAt(token);
        if (issued != null && account.getLogoutAt() != null) {
            LocalDateTime issuedAt = LocalDateTime.ofInstant(issued.toInstant(), ZoneId.systemDefault());
            if (issuedAt.isBefore(account.getLogoutAt())) {
                throw new IllegalArgumentException("Token has been invalidated (user logged out)");
            }
        }

        User user = account.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Associated user not found");
        }

        MeResponse resp = new MeResponse();
        resp.setId(user.getId());
        resp.setName(user.getName());
        resp.setEmail(account.getEmail());
        resp.setPhone(user.getPhone());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setBackgroundUrl(user.getBackgroundUrl());
        resp.setActive(user.isActive());
        resp.setAddress(user.getAddress());
        resp.setRole(account.getRole());
        resp.setVerified(account.isVerified());
        resp.setLastLogin(account.getLastLogin());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());

        return resp;
    }

    // new: public method to get public profile by userId (moved from controller)
    public UserPublicResponse getPublicUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Float rating = null;
        if (user instanceof Owner) {
            rating = ((Owner) user).getRating();
        }

        return new UserPublicResponse(
                user.getId(),
                user.getName(),
                user.getAvatarUrl(),
                user.getBackgroundUrl(),
                user.isActive(),
                user.getAddress(),
                rating,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // new: update user profile (only the owner can update their profile)
    @Transactional
    public MeResponse updateUser(Integer userId, String authorizationHeader, UpdateUserRequest req) {
        if (req == null) throw new IllegalArgumentException("Request body is required");

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token does not contain email");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // check logout timestamp
        Date issued = jwtService.extractIssuedAt(token);
        if (issued != null && account.getLogoutAt() != null) {
            LocalDateTime issuedAt = LocalDateTime.ofInstant(issued.toInstant(), ZoneId.systemDefault());
            if (issuedAt.isBefore(account.getLogoutAt())) {
                throw new IllegalArgumentException("Token has been invalidated (user logged out)");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Only allow the owner of the account (account.user.id) to update their profile
        if (account.getUser() == null || !account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to update this user");
        }

        // apply non-null fields from request
        if (req.getName() != null) user.setName(req.getName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        AddressRequest addrReq = req.getAddressRequest();

        if (addrReq != null) {
            // 1. Lấy địa chỉ hiện tại từ DB (nếu chưa có thì tạo mới)
            Address currentAddr = user.getAddress();
            if (currentAddr == null) {
                currentAddr = new Address();
            }

            // 2. MERGE: Cập nhật text trước (lấy cái mới đè lên cái cũ)
            if (addrReq.getStreet() != null) currentAddr.setStreet(addrReq.getStreet());
            if (addrReq.getDistrict() != null) currentAddr.setDistrict(addrReq.getDistrict());
            if (addrReq.getCity() != null) currentAddr.setCity(addrReq.getCity());
            if (addrReq.getProvince() != null) currentAddr.setProvince(addrReq.getProvince());

            List<String> addressParts = new ArrayList<>();
            if (hasText(currentAddr.getStreet())) addressParts.add(currentAddr.getStreet());
            if (hasText(currentAddr.getDistrict())) addressParts.add(currentAddr.getDistrict());
            if (hasText(currentAddr.getCity())) addressParts.add(currentAddr.getCity());
            if (hasText(currentAddr.getProvince())) addressParts.add(currentAddr.getProvince());

            // 4. Gọi API lấy tọa độ (Chỉ gọi khi có địa chỉ)
            if (!addressParts.isEmpty()) {
                String fullAddressToGeocode = String.join(", ", addressParts);
                try {
                    GoongResponse.Location location = goongMapService.getGeoLocation(fullAddressToGeocode);

                    if (location != null) {
                        // Cập nhật tọa độ mới vào object
                        currentAddr.setLatitude(location.getLat());
                        currentAddr.setLongitude(location.getLng());
                    }
                } catch (Exception e) {
                    // Log lỗi (ví dụ mạng lag), giữ nguyên tọa độ cũ, không throw exception
                    System.err.println("Warning: Failed to update geolocation: " + e.getMessage());
                }
            }

            // 5. Set ngược lại vào User để chuẩn bị save
            user.setAddress(currentAddr);
        }
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        // prepare MeResponse
        MeResponse resp = new MeResponse();
        resp.setId(user.getId());
        resp.setName(user.getName());
        resp.setEmail(account.getEmail());
        resp.setPhone(user.getPhone());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setBackgroundUrl(user.getBackgroundUrl());
        resp.setActive(user.isActive());
        resp.setAddress(user.getAddress());
        resp.setRole(account.getRole());
        resp.setVerified(account.isVerified());
        resp.setLastLogin(account.getLastLogin());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());

        return resp;
    }
    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // new: change password
    @Transactional
    public String changePassword(String authorizationHeader, ChangePasswordRequest req) {
        if (req == null) throw new IllegalArgumentException("Request body is required");
        if (req.getOldPassword() == null || req.getOldPassword().isBlank()) throw new IllegalArgumentException("Old password is required");
        if (req.getNewPassword() == null || req.getNewPassword().isBlank()) throw new IllegalArgumentException("New password is required");
        if (req.getConfirmNewPassword() == null || req.getConfirmNewPassword().isBlank()) throw new IllegalArgumentException("Confirm new password is required");

        if (!req.getNewPassword().equals(req.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token does not contain email");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // check logout timestamp
        Date issued = jwtService.extractIssuedAt(token);
        if (issued != null && account.getLogoutAt() != null) {
            LocalDateTime issuedAt = LocalDateTime.ofInstant(issued.toInstant(), ZoneId.systemDefault());
            if (issuedAt.isBefore(account.getLogoutAt())) {
                throw new IllegalArgumentException("Token has been invalidated (user logged out)");
            }
        }

        // verify old password
        if (!passwordEncoder.matches(req.getOldPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        // optional: prevent reusing same password
        if (passwordEncoder.matches(req.getNewPassword(), account.getPassword())) {
            throw new IllegalArgumentException("New password must be different from old password");
        }

        account.setPassword(passwordEncoder.encode(req.getNewPassword()));
        accountRepository.save(account);

        return "Changed password";
    }

    // Forgot-password: send OTP to email if account exists
    public String forgotPassword(ForgotPasswordRequest req) {
        String email = req.getEmail();
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");

        var accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) throw new IllegalArgumentException("Account not found");

        // rate limit
        if (!verificationRateLimiter.allow(email)) {
            throw new IllegalArgumentException("Too many attempts. Please try again later.");
        }

        Account account = accountOpt.get();
        String code = generateVerificationCode();
        account.setVerifyCode(code);
        account.setVerifyCodeExpiry(LocalDateTime.now().plusMinutes(VERIFY_CODE_EXPIRY_MINUTES));
        accountRepository.save(account);

        // send email (best-effort)
        emailService.sendVerificationCode(email, code);

        return "Verification code sent";
    }

    // Verify OTP for forgot-password and allow password reset for a short window
    public String verifyForgotPassword(VerifyRequest req) {
        String email = req.getEmail();
        String code = req.getCode();
        if (email == null || email.isBlank() || code == null || code.isBlank()) throw new IllegalArgumentException("Email and code are required");

        Account account = accountRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Account not found"));

        LocalDateTime expiry = account.getVerifyCodeExpiry();
        if (expiry == null || expiry.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code expired. Please request a new code.");
        }

        if (!code.equals(account.getVerifyCode())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        // allow reset for a short period
        account.setPasswordResetAllowedUntil(LocalDateTime.now().plusMinutes(VERIFY_CODE_EXPIRY_MINUTES));
        account.setVerifyCode(null);
        account.setVerifyCodeExpiry(null);
        accountRepository.save(account);

        return "Verified";
    }

    // Reset password after successful OTP verification (no auth header required)
    @Transactional
    public String resetForgotPassword(ForgotPasswordResetRequest req) {
        String email = req.getEmail();
        String newPassword = req.getNewPassword();
        String confirm = req.getConfirmNewPassword();
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (newPassword == null || newPassword.isBlank()) throw new IllegalArgumentException("New password is required");
        if (confirm == null || confirm.isBlank()) throw new IllegalArgumentException("Confirm new password is required");
        if (!newPassword.equals(confirm)) throw new IllegalArgumentException("New password and confirm password do not match");

        Account account = accountRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Account not found"));

        LocalDateTime allowedUntil = account.getPasswordResetAllowedUntil();
        if (allowedUntil == null || allowedUntil.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset not allowed or reset window expired. Please verify OTP first.");
        }

        // optional: prevent reusing same password
        if (passwordEncoder.matches(newPassword, account.getPassword())) {
            throw new IllegalArgumentException("New password must be different from old password");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setPasswordResetAllowedUntil(null);
        accountRepository.save(account);

        return "Password reset";
    }
}
