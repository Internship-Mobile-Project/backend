package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacilityResponse {
    private Integer id;
    private String name;
    private String description;
    private Double price;
}
