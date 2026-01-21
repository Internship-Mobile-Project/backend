package com.badminton.shop.ws_booking_sport.admin.controller;

import com.badminton.shop.ws_booking_sport.admin.service.AdminService;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import com.badminton.shop.ws_booking_sport.model.core.User;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminController {

    private final AdminService adminService;

    // API lấy danh sách User
    // GET: /api/admin/users?page=0&size=10
    @GetMapping("/users")
    public ResponseEntity<DataResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<User> users = adminService.getAllUsers(pageable);

        DataResponse body = DataResponse.success(users, "Users fetched successfully", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // API Khóa/Mở khóa User
    // PUT: /api/admin/users/1/toggle-status
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<DataResponse> toggleUserStatus(@PathVariable Integer id) {
        User updatedUser = adminService.toggleUserStatus(id);

        String message = updatedUser.isActive() ? "User activated successfully" : "User deactivated successfully";

        DataResponse body = DataResponse.success(updatedUser, message, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
    @GetMapping("/venues")
    public ResponseEntity<DataResponse> getAllVenues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Venue> venues = adminService.getAllVenues(PageRequest.of(page, size));
        DataResponse body = DataResponse.success(venues, "All venues fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // 2. API Lấy tất cả Booking (Đơn đặt)
    @GetMapping("/bookings")
    public ResponseEntity<DataResponse> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Booking> bookings = adminService.getAllBookings(PageRequest.of(page, size));
        DataResponse body = DataResponse.success(bookings, "All bookings fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}