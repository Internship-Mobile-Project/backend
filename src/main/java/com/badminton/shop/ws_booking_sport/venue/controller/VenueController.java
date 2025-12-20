package com.badminton.shop.ws_booking_sport.venue.controller;

import com.badminton.shop.ws_booking_sport.dto.request.AddVenueRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateVenueRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateMainImageRequest;
import com.badminton.shop.ws_booking_sport.dto.response.VenueResponse;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<DataResponse> createVenue(@RequestBody AddVenueRequest req, HttpServletRequest request) {
        VenueResponse resp = venueService.createVenue(req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Venue created", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse> getVenue(@PathVariable Integer id) {
        VenueResponse resp = venueService.getVenue(id);
        DataResponse body = DataResponse.success(resp, "Venue fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse> updateVenue(@PathVariable Integer id, @RequestBody UpdateVenueRequest req, HttpServletRequest request) {
        VenueResponse resp = venueService.updateVenue(id, req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Venue updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // update main image (set/replace first image) - owner/admin only
    @PutMapping("/{id}/main-image")
    public ResponseEntity<DataResponse> updateMainImage(@PathVariable Integer id, @RequestBody UpdateMainImageRequest req, HttpServletRequest request) {
        VenueResponse resp = venueService.updateMainImage(id, req.getImageUrl(), request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Main image updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // list images for a venue (public)
    @GetMapping("/{id}/images")
    public ResponseEntity<DataResponse> listImages(@PathVariable Integer id) {
        List<String> imgs = venueService.listImages(id);
        DataResponse body = DataResponse.success(imgs, "Images fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse> deleteVenue(@PathVariable Integer id, HttpServletRequest request) {
        String resp = venueService.deleteVenue(id, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<DataResponse> listByOwner(@PathVariable Integer ownerId) {
        List<VenueResponse> list = venueService.listByOwner(ownerId);
        DataResponse body = DataResponse.success(list, "Venues fetched for owner", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}
