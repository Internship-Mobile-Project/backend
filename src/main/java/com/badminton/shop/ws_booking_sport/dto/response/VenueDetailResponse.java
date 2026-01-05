package com.badminton.shop.ws_booking_sport.dto.response;

import com.badminton.shop.ws_booking_sport.dto.request.AddressRequest;
import com.badminton.shop.ws_booking_sport.model.core.Address;
import com.badminton.shop.ws_booking_sport.model.venue.Facility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueDetailResponse {
    private Integer id;
    private String name;
    private String description;
    private String sport;
    private AddressRequest address;
    private LocalTime timeOpen;
    private LocalTime timeClose;
    private List<String> imageUrls;
    private String mainImageUrl;
    private List<ReviewResponse> reviews;
    private List<FieldResponse> fields;
    private List<FacilityResponse> facilities;
    private Integer ownerId;
    private float rating;
}
