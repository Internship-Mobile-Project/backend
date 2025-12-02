package com.badminton.shop.ws_booking_sport.core.controller;

import com.badminton.shop.ws_booking_sport.core.service.UserService;
import com.badminton.shop.ws_booking_sport.dto.request.RegisterRequest;
import com.badminton.shop.ws_booking_sport.dto.request.RefreshRequest;
import com.badminton.shop.ws_booking_sport.dto.request.AuthRequest;
import com.badminton.shop.ws_booking_sport.dto.request.VerifyRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ResendVerifyRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ChangePasswordRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ForgotPasswordRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ForgotPasswordResetRequest;
import com.badminton.shop.ws_booking_sport.dto.response.RegisterResponse;
import com.badminton.shop.ws_booking_sport.dto.response.RefreshResponse;
import com.badminton.shop.ws_booking_sport.dto.response.AuthResponse;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

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
