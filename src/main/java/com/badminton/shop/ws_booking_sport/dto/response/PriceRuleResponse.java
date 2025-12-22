package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Data;
import java.time.LocalTime;

@Data
public class PriceRuleResponse {
    private Integer id;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private double pricePerHour;
    private Integer fieldId;
}

