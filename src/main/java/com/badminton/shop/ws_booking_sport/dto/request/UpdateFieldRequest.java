package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;

@Data
public class UpdateFieldRequest {
    private String name;
    private String type;
    private Double pricePerHour;
}
