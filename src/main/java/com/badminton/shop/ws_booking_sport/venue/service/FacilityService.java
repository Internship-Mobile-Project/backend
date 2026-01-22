package com.badminton.shop.ws_booking_sport.venue.service;

import com.badminton.shop.ws_booking_sport.dto.request.CreateFacilityRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateFacilityRequest;
import com.badminton.shop.ws_booking_sport.dto.response.FacilityResponse;

import java.util.List;

public interface FacilityService {
    FacilityResponse create(Integer venueId, CreateFacilityRequest req, String authHeader);
    List<FacilityResponse> listByVenue(Integer venueId);
    FacilityResponse getById(Integer id);
    FacilityResponse update(Integer id, UpdateFacilityRequest req, String authHeader);
    String delete(Integer id, String authHeader);
}

