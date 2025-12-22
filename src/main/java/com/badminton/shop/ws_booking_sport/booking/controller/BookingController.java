package com.badminton.shop.ws_booking_sport.booking.controller;


import com.badminton.shop.ws_booking_sport.booking.service.BookingService;
import com.badminton.shop.ws_booking_sport.booking.service.SlotService;
import com.badminton.shop.ws_booking_sport.dto.request.BookingRequest;
import com.badminton.shop.ws_booking_sport.dto.request.PaymentUpdateRequest;
import com.badminton.shop.ws_booking_sport.dto.response.BookingResponse;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.dto.response.UnavailableSlotResponse;
import com.badminton.shop.ws_booking_sport.enums.PaymentStatus;
import com.badminton.shop.ws_booking_sport.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final SlotService slotService;
    private final JwtService jwtService;

    @PostMapping("/api/bookings")
    public ResponseEntity<DataResponse> createBooking(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                      @RequestBody BookingRequest req) {
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

        var booking = bookingService.createBookingForUser(req, userId);
        // map entity to DTO before returning to client
        var bookingDto = bookingService.toBookingResponse(booking);
        DataResponse body = DataResponse.success(bookingDto, "Booking created", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // POST /api/bookings/{bookingId}/online-payment -> returns redirect url for customer to pay
    @PostMapping("/api/bookings/{bookingId}/online-payment")
    public ResponseEntity<DataResponse> initiateOnlinePayment(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String bookingId
    ) {
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

        String redirectUrl = bookingService.initiateOnlinePayment(bookingId, userId);
        DataResponse body = DataResponse.success(redirectUrl, "Redirect URL generated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // PUT /api/bookings/{bookingId}/owner/payment -> owner updates cash payment status
    @PutMapping("/api/bookings/{bookingId}/owner/payment")
    public ResponseEntity<DataResponse> ownerUpdateCashPaymentStatus(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String bookingId,
            @RequestBody PaymentUpdateRequest req
    ) {
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
        int ownerId;
        try {
            ownerId = Integer.parseInt(subject);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user id in token subject");
        }

        PaymentStatus status;
        try {
            status = PaymentStatus.valueOf(req.getStatus().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid payment status");
        }

        var updated = bookingService.ownerUpdateCashPaymentStatus(bookingId, ownerId, status);
        BookingResponse dto = bookingService.toBookingResponse(updated);
        DataResponse body = DataResponse.success(dto, "Payment status updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // GET /api/fields/{fieldId}/unavailable-slots?date=YYYY-MM-DD
    @GetMapping("/api/fields/{fieldId}/unavailable-slots")
    public ResponseEntity<DataResponse> getUnavailableSlots(
            @PathVariable Integer fieldId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<UnavailableSlotResponse> slots = slotService.getUnavailableSlots(fieldId, date);
        DataResponse body = DataResponse.success(slots, "Unavailable slots fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // small DTO used by owner endpoint

}
