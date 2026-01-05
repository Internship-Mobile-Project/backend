package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateVenueRequest {
    private String name;
    private String description;
    private AddressRequest address;
    private LocalTime timeOpen;
    private LocalTime timeClose;
    private String sport;
}

