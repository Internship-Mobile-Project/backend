package com.badminton.shop.ws_booking_sport.core.service;

import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.core.repository.UserRepository;
import com.badminton.shop.ws_booking_sport.dto.request.RegisterRequest;
import com.badminton.shop.ws_booking_sport.dto.request.RefreshRequest;
import com.badminton.shop.ws_booking_sport.dto.request.AuthRequest;
import com.badminton.shop.ws_booking_sport.dto.request.VerifyRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ResendVerifyRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateUserRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ChangePasswordRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ForgotPasswordRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ForgotPasswordResetRequest;
import com.badminton.shop.ws_booking_sport.dto.response.RegisterResponse;
import com.badminton.shop.ws_booking_sport.dto.response.RefreshResponse;
import com.badminton.shop.ws_booking_sport.dto.response.AuthResponse;
import com.badminton.shop.ws_booking_sport.dto.response.MeResponse;
import com.badminton.shop.ws_booking_sport.dto.response.UserPublicResponse;
import com.badminton.shop.ws_booking_sport.enums.Role;
import com.badminton.shop.ws_booking_sport.model.core.Account;
import com.badminton.shop.ws_booking_sport.model.core.Customer;
import com.badminton.shop.ws_booking_sport.model.core.User;
import com.badminton.shop.ws_booking_sport.model.core.Owner;
import com.badminton.shop.ws_booking_sport.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final VerificationRateLimiter verificationRateLimiter;

    private static final int VERIFY_CODE_EXPIRY_MINUTES = 15; // verify code valid for 15 minutes

    private String generateVerificationCode() {
        Random rnd = new Random();
        int number = 100000 + rnd.nextInt(900000);
        return String.valueOf(number);
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

        return new AuthResponse(user.getId(), user.getName(), account.getEmail(), access, refresh, role);
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
        if (req.getBackgroundUrl() != null) user.setBackgroundUrl(req.getBackgroundUrl());
        if (req.getAddress() != null) user.setAddress(req.getAddress());

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
