package com.badminton.shop.ws_booking_sport.dto.response;

import com.badminton.shop.ws_booking_sport.model.core.Address;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class VenueResponse {
    private Integer id;
    private String name;
    private String description;
    private String sport;
    private Address address;
    private LocalTime timeOpen;
    private LocalTime timeClose;
    private List<String> imageUrls;
    private Integer ownerId;
    private float rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
