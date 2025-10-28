package com.badminton.shop.ws_booking_sport.controller;

import com.badminton.shop.ws_booking_sport.dto.request.LoginRequest;
import com.badminton.shop.ws_booking_sport.dto.request.RegisterRequest;
import com.badminton.shop.ws_booking_sport.dto.request.RefreshRequest;
import com.badminton.shop.ws_booking_sport.dto.response.LoginResponse;
import com.badminton.shop.ws_booking_sport.model.User;
import com.badminton.shop.ws_booking_sport.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        LoginResponse res = authService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest req) {
        int role = req.getRole() != null ? req.getRole() : 1;
        User created = authService.register(req.getEmail(), req.getPassword(), role);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest req) {
        LoginResponse res = authService.refreshToken(req.getRefreshToken());
        return ResponseEntity.ok(res);
    }
}
