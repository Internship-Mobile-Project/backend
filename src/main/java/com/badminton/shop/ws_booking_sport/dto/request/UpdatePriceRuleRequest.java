package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;
import java.time.LocalTime;

@Data
public class UpdatePriceRuleRequest {
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double pricePerHour;
}

