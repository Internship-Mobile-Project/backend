package com.badminton.shop.ws_booking_sport.dto.request;

import com.badminton.shop.ws_booking_sport.model.core.Address;
import lombok.Data;

import java.time.LocalTime;

@Data
public class AddVenueRequest {
    private String name;
    private String description;
    private AddressRequest address;
    private LocalTime timeOpen;
    private LocalTime timeClose;
    private String sport; // e.g. badminton, multiSport
    private Double pricePerHour; // venue-level price
}
