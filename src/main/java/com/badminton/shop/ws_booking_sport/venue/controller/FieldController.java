package com.badminton.shop.ws_booking_sport.venue.controller;

import com.badminton.shop.ws_booking_sport.dto.request.CreateFieldRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateFieldRequest;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.dto.response.FieldResponse;
import com.badminton.shop.ws_booking_sport.venue.service.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FieldController {

    private final FieldService fieldService;

    // create a field for a venue (owner/admin)
    @PostMapping("/api/venues/{venueId}/fields")
    public ResponseEntity<DataResponse> createField(@PathVariable Integer venueId, @RequestBody CreateFieldRequest req, HttpServletRequest request) {
        FieldResponse resp = fieldService.createField(venueId, req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Field created", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // get a field by id (public)
    @GetMapping("/api/fields/{id}")
    public ResponseEntity<DataResponse> getField(@PathVariable Integer id) {
        FieldResponse resp = fieldService.getField(id);
        DataResponse body = DataResponse.success(resp, "Field fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // update a field (owner/admin)
    @PutMapping("/api/fields/{id}")
    public ResponseEntity<DataResponse> updateField(@PathVariable Integer id, @RequestBody UpdateFieldRequest req, HttpServletRequest request) {
        FieldResponse resp = fieldService.updateField(id, req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Field updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // delete a field (owner/admin)
    @DeleteMapping("/api/fields/{id}")
    public ResponseEntity<DataResponse> deleteField(@PathVariable Integer id, HttpServletRequest request) {
        String resp = fieldService.deleteField(id, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // list fields by venue (public)
    @GetMapping("/api/venues/{venueId}/fields")
    public ResponseEntity<DataResponse> listByVenue(@PathVariable Integer venueId) {
        List<FieldResponse> list = fieldService.listByVenue(venueId);
        DataResponse body = DataResponse.success(list, "Fields fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

}
