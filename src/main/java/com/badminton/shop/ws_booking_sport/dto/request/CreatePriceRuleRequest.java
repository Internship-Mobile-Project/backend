package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;
import java.time.LocalTime;

@Data
public class CreatePriceRuleRequest {
    private int dayOfWeek; // 1=Monday .. 7=Sunday (follow your convention)
    private LocalTime startTime;
    private LocalTime endTime;
    private double pricePerHour;
}

