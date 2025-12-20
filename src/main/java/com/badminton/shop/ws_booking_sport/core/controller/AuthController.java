package com.badminton.shop.ws_booking_sport.core.controller;

import com.badminton.shop.ws_booking_sport.core.service.UserService;
import com.badminton.shop.ws_booking_sport.dto.request.*;
import com.badminton.shop.ws_booking_sport.dto.response.RegisterResponse;
import com.badminton.shop.ws_booking_sport.dto.response.RefreshResponse;
import com.badminton.shop.ws_booking_sport.dto.response.AuthResponse;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<DataResponse> register(@RequestBody RegisterRequest req) {
        RegisterResponse resp = userService.register(req);
        DataResponse body = DataResponse.success(resp, "User registered", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/register/owner")
    public ResponseEntity<DataResponse> registerOwner(@RequestBody RegisterRequest req) {
        RegisterResponse resp = userService.registerOwner(req);
        DataResponse body = DataResponse.success(resp, "Owner registered", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<DataResponse> login(@RequestBody AuthRequest req) {
        AuthResponse resp = userService.login(req);
        DataResponse body = DataResponse.success(resp, "Login successful", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
    @PostMapping("/google-login")
        public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
            try {
                // Gọi service xử lý
                AuthResponse response = userService.authenticateGoogle(request.getIdToken());
                return ResponseEntity.ok(response);

            } catch (IllegalArgumentException e) {
                // Lỗi do token không hợp lệ (trả về 401)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());

            } catch (GeneralSecurityException | IOException e) {
                // Lỗi kỹ thuật khi verify với Google (trả về 500)
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to verify Google ID token");

            } catch (Exception e) {
                // Các lỗi khác
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication error");
            }
        }

    @PostMapping("/refresh")
    public ResponseEntity<DataResponse> refresh(@RequestBody RefreshRequest req) {
        RefreshResponse resp = userService.refresh(req);
        DataResponse body = DataResponse.success(resp, "Token refreshed", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify/send")
    public ResponseEntity<DataResponse> resendVerification(@RequestBody ResendVerifyRequest req) {
        userService.resendVerification(req);
        DataResponse body = DataResponse.success(null, "Verification code sent", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify/confirm")
    public ResponseEntity<DataResponse> confirmVerification(@RequestBody VerifyRequest req) {
        String resp = userService.confirmVerification(req);
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<DataResponse> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        userService.logout(authHeader);
        DataResponse body = DataResponse.success(null, "Logged out", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/change-password")
    public ResponseEntity<DataResponse> changePassword(@RequestBody ChangePasswordRequest req, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String resp = userService.changePassword(authHeader, req);
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // Forgot-password endpoints
    @PostMapping("/forgot-password")
    public ResponseEntity<DataResponse> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        String resp = userService.forgotPassword(req);
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<DataResponse> verifyForgotPassword(@RequestBody VerifyRequest req) {
        String resp = userService.verifyForgotPassword(req);
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<DataResponse> resetForgotPassword(@RequestBody ForgotPasswordResetRequest req) {
        String resp = userService.resetForgotPassword(req);
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}
