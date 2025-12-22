package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Data;

@Data
public class FieldResponse {
    private Integer id;
    private String name;
    private String type;
    private Integer venueId;
}

