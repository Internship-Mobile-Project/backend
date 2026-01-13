package com.badminton.shop.ws_booking_sport.venue.controller;

import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PriceRuleController {

    // All price rule endpoints have been removed in favor of a single pricePerHour on Field.
    private static final String MESSAGE = "PriceRule API removed. Use Field.pricePerHour instead.";

    @RequestMapping(value = "/api/fields/{fieldId}/pricerules", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<DataResponse> unavailableForField(@PathVariable(required = false) Integer fieldId) {
        DataResponse body = DataResponse.success(null, MESSAGE, HttpStatus.GONE.value());
        return ResponseEntity.status(HttpStatus.GONE).body(body);
    }

    @RequestMapping(value = "/api/pricerules/{id}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<DataResponse> unavailableForId(@PathVariable(required = false) Integer id) {
        DataResponse body = DataResponse.success(null, MESSAGE, HttpStatus.GONE.value());
        return ResponseEntity.status(HttpStatus.GONE).body(body);
    }
}
