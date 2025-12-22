package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingRequest {
    private Integer venueId;
    private Integer fieldId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer customerId;
    private String paymentMethod;
    private String paymentProvider;
}
