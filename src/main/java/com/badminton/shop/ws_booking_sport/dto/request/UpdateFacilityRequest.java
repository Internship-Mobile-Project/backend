package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFacilityRequest {
    private String name;
    private String description;
    private Double price;
}

