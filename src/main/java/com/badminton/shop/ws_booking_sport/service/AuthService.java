package com.badminton.shop.ws_booking_sport.service;

import com.badminton.shop.ws_booking_sport.dto.response.LoginResponse;
import com.badminton.shop.ws_booking_sport.model.User;
import com.badminton.shop.ws_booking_sport.model.Role;
import com.badminton.shop.ws_booking_sport.repository.UserRepository;
import com.badminton.shop.ws_booking_sport.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final long refreshExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${jwt.refreshExpiration:1209600000}") long refreshExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public LoginResponse login(String email, String password) {
        // authenticate user (we don't need the returned Authentication object here)
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        // load user to read role and embed it into token
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found after authentication"));
        String roleName = Role.fromCode(user.getRole()).name();

        String accessToken = jwtUtil.generateToken(email, roleName);
        String refreshToken = jwtUtil.generateToken(email, roleName, refreshExpirationMs);

        return new LoginResponse(accessToken, refreshToken);
    }

    public LoginResponse refreshToken(String refreshTokenStr) {
        // validate refresh JWT
        if (!jwtUtil.validateToken(refreshTokenStr)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String subject = jwtUtil.getSubject(refreshTokenStr);
        String roleName = jwtUtil.getRoleFromToken(refreshTokenStr);

        // issue new access token and a new refresh token (rotation)
        String newAccessToken = jwtUtil.generateToken(subject, roleName);
        String newRefreshToken = jwtUtil.generateToken(subject, roleName, refreshExpirationMs);

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    public User register(String email, String password, int role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        String encoded = passwordEncoder.encode(password);
        User user = new User(email, encoded, role);
        return userRepository.save(user);
    }
}
