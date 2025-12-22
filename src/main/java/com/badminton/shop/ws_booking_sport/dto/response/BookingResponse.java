package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private String id;
    private Integer customerId;
    private String customerName;
    private Integer fieldId;
    private String fieldName;
    private String status;
    private double totalPrice;
    private LocalDateTime bookedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SlotInfo> slots;

    // payment details returned to client
    private PaymentInfo payment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotInfo {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private double finalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String method; // CASH, VNPAY, MOMO, etc.
        private String redirectUrl; // for online payments
        private double amount;
        private String currency;
        private String status; // PENDING, SUCCESS, etc.
    }
}
