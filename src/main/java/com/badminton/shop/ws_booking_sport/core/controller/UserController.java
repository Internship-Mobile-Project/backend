package com.badminton.shop.ws_booking_sport.core.controller;

import com.badminton.shop.ws_booking_sport.core.service.UserService;
import com.badminton.shop.ws_booking_sport.dto.request.RegisterRequest;
import com.badminton.shop.ws_booking_sport.dto.request.RefreshRequest;
import com.badminton.shop.ws_booking_sport.dto.request.AuthRequest;
import com.badminton.shop.ws_booking_sport.dto.request.VerifyRequest;
import com.badminton.shop.ws_booking_sport.dto.request.ResendVerifyRequest;
import com.badminton.shop.ws_booking_sport.dto.response.RegisterResponse;
import com.badminton.shop.ws_booking_sport.dto.response.RefreshResponse;
import com.badminton.shop.ws_booking_sport.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        RegisterResponse resp = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/register/owner")
    public ResponseEntity<?> registerOwner(@RequestBody RegisterRequest req) {
        RegisterResponse resp = userService.registerOwner(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        AuthResponse resp = userService.login(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        RefreshResponse resp = userService.refresh(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify/send")
    public ResponseEntity<?> resendVerification(@RequestBody ResendVerifyRequest req) {
        userService.resendVerification(req);
        return ResponseEntity.ok("Verification code sent");
    }

    @PostMapping("/verify/confirm")
    public ResponseEntity<?> confirmVerification(@RequestBody VerifyRequest req) {
        String resp = userService.confirmVerification(req);
        return ResponseEntity.ok(resp);
    }
}
