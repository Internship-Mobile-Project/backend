package com.badminton.shop.ws_booking_sport.core.controller;

import com.badminton.shop.ws_booking_sport.core.service.UserService;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateUserRequest;
import com.badminton.shop.ws_booking_sport.dto.response.UserPublicResponse;
import com.badminton.shop.ws_booking_sport.dto.response.MeResponse;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<DataResponse> getPublicUser(@PathVariable Integer userId) {
        UserPublicResponse resp = userService.getPublicUser(userId);
        DataResponse body = DataResponse.success(resp, "User fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<DataResponse> updateUser(@PathVariable Integer userId, @RequestBody UpdateUserRequest req, HttpServletRequest request) {
        MeResponse resp = userService.updateUser(userId, request.getHeader("Authorization"), req);
        DataResponse body = DataResponse.success(resp, "User updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me")
    public ResponseEntity<DataResponse> me(HttpServletRequest request) {
        MeResponse resp = userService.me(request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Me fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}
