package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnavailableSlotResponse {
    private LocalTime startTime;
    private LocalTime endTime;
}
