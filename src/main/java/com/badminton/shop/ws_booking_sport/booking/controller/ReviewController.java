package com.badminton.shop.ws_booking_sport.booking.controller;

import com.badminton.shop.ws_booking_sport.dto.request.ReviewRequest;
import com.badminton.shop.ws_booking_sport.dto.response.ReviewResponse;
import com.badminton.shop.ws_booking_sport.booking.service.ReviewService;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtService jwtService;

    // POST /api/bookings/{bookingId}/reviews - create review by booking's customer (booking must be COMPLETED)
    @PostMapping("/api/bookings/{bookingId}/reviews")
    public ResponseEntity<DataResponse> createReview(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                     @PathVariable String bookingId,
                                                     @RequestBody ReviewRequest req) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String subject = jwtService.extractSubject(token);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Token does not contain user id");
        }
        int userId;
        try {
            userId = Integer.parseInt(subject);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user id in token subject");
        }

        var created = reviewService.createReview(bookingId, req, userId);
        ReviewResponse dto = reviewService.toResponse(created);
        DataResponse body = DataResponse.success(dto, "Review created", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // GET /api/bookings/{bookingId}/reviews - list reviews for a booking
    @GetMapping("/api/bookings/{bookingId}/reviews")
    public ResponseEntity<DataResponse> listByBooking(@PathVariable String bookingId) {
        List<ReviewResponse> list = reviewService.listByBooking(bookingId);
        DataResponse body = DataResponse.success(list, "Reviews fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // GET /api/reviews/{id}
    @GetMapping("/api/reviews/{id}")
    public ResponseEntity<DataResponse> getById(@PathVariable String id) {
        ReviewResponse r = reviewService.getById(id);
        DataResponse body = DataResponse.success(r, "Review fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // PUT /api/reviews/{id} - update review by owner (customer)
    @PutMapping("/api/reviews/{id}")
    public ResponseEntity<DataResponse> updateReview(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                     @PathVariable String id,
                                                     @RequestBody ReviewRequest req) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String subject = jwtService.extractSubject(token);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Token does not contain user id");
        }
        int userId;
        try {
            userId = Integer.parseInt(subject);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user id in token subject");
        }

        var updated = reviewService.updateReview(id, req, userId);
        ReviewResponse dto = reviewService.toResponse(updated);
        DataResponse body = DataResponse.success(dto, "Review updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // DELETE /api/reviews/{id} - delete review by owner (customer)
    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<DataResponse> deleteReview(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                     @PathVariable String id) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String subject = jwtService.extractSubject(token);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Token does not contain user id");
        }
        int userId;
        try {
            userId = Integer.parseInt(subject);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user id in token subject");
        }

        reviewService.deleteReview(id, userId);
        DataResponse body = DataResponse.success(null, "Review deleted", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}

