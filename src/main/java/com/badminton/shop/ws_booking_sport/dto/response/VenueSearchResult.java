package com.badminton.shop.ws_booking_sport.dto.response;

import com.badminton.shop.ws_booking_sport.model.core.Address;
import lombok.Data;

@Data
public class VenueSearchResult {
    private Integer id;
    private String name;
    private String sport;
    private float rating;
    private Address address;
    private Double latitude;
    private Double longitude;
    private Double distanceKm; // distance from search center
    private String formattedAddress; // from Goong
}

