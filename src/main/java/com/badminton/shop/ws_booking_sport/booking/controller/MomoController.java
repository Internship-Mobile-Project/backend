package com.badminton.shop.ws_booking_sport.booking.controller;

import com.badminton.shop.ws_booking_sport.booking.service.MomoService;
import com.badminton.shop.ws_booking_sport.dto.request.CreateMomoOrderRequest;
import com.badminton.shop.ws_booking_sport.dto.response.MomoCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/momo")
@RequiredArgsConstructor
public class MomoController {

    private final MomoService momoService;

    @PostMapping("/create")
    public ResponseEntity<?> createMomoOrder(@RequestBody CreateMomoOrderRequest req) {
        try {
            MomoCreateResponse res = momoService.createOrder(req.getAmount(), req.getOrderInfo(), req.getOrderId(), req.getReturnUrl());
            if (res.getErrorCode() != null && res.getErrorCode() != 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/ipn")
    public ResponseEntity<?> momoNotify(@RequestBody Map<String, Object> payload) {
        // TODO: Verify signature and update order status in database
        // For now, logging that we received the IPN
        System.out.println("Received Momo IPN: " + payload);

        // Return 204 No Content to acknowledge
        return ResponseEntity.noContent().build();
    }
}

