package com.badminton.shop.ws_booking_sport.venue.controller;

import com.badminton.shop.ws_booking_sport.dto.request.CreateFacilityRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateFacilityRequest;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.dto.response.FacilityResponse;
import com.badminton.shop.ws_booking_sport.venue.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    // create facility for a venue (owner/admin)
    @PostMapping("/api/venues/{venueId}/facilities")
    public ResponseEntity<DataResponse> create(@PathVariable Integer venueId, @RequestBody CreateFacilityRequest req, HttpServletRequest request) {
        FacilityResponse resp = facilityService.create(venueId, req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Facility created", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // list facilities by venue (public)
    @GetMapping("/api/venues/{venueId}/facilities")
    public ResponseEntity<DataResponse> listByVenue(@PathVariable Integer venueId) {
        List<FacilityResponse> list = facilityService.listByVenue(venueId);
        DataResponse body = DataResponse.success(list, "Facilities fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // get facility by id (public)
    @GetMapping("/api/facilities/{id}")
    public ResponseEntity<DataResponse> getById(@PathVariable Integer id) {
        FacilityResponse resp = facilityService.getById(id);
        DataResponse body = DataResponse.success(resp, "Facility fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // update facility (owner/admin)
    @PutMapping("/api/facilities/{id}")
    public ResponseEntity<DataResponse> update(@PathVariable Integer id, @RequestBody UpdateFacilityRequest req, HttpServletRequest request) {
        FacilityResponse resp = facilityService.update(id, req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Facility updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // delete facility (owner/admin)
    @DeleteMapping("/api/facilities/{id}")
    public ResponseEntity<DataResponse> delete(@PathVariable Integer id, HttpServletRequest request) {
        String resp = facilityService.delete(id, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}

