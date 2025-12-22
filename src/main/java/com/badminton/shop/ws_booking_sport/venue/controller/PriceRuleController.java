package com.badminton.shop.ws_booking_sport.venue.controller;

import com.badminton.shop.ws_booking_sport.dto.request.CreatePriceRuleRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdatePriceRuleRequest;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.dto.response.PriceRuleResponse;
import com.badminton.shop.ws_booking_sport.venue.service.PriceRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PriceRuleController {

    private final PriceRuleService priceRuleService;

    // create price rule for a field (owner/admin)
    @PostMapping("/api/fields/{fieldId}/pricerules")
    public ResponseEntity<DataResponse> createPriceRule(@PathVariable Integer fieldId, @RequestBody CreatePriceRuleRequest req, HttpServletRequest request) {
        PriceRuleResponse resp = priceRuleService.createPriceRule(fieldId, req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "PriceRule created", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // batch add price rules to a field (owner/admin)
    @PostMapping("/api/fields/{fieldId}/pricerules/batch")
    public ResponseEntity<DataResponse> addPriceRules(@PathVariable Integer fieldId, @RequestBody List<CreatePriceRuleRequest> reqs, HttpServletRequest request) {
        List<PriceRuleResponse> resp = priceRuleService.addPriceRules(fieldId, reqs, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "PriceRules created", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // get a price rule by id (public)
    @GetMapping("/api/pricerules/{id}")
    public ResponseEntity<DataResponse> getPriceRule(@PathVariable Integer id) {
        PriceRuleResponse resp = priceRuleService.getPriceRule(id);
        DataResponse body = DataResponse.success(resp, "PriceRule fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // update a price rule (owner/admin)
    @PutMapping("/api/pricerules/{id}")
    public ResponseEntity<DataResponse> updatePriceRule(@PathVariable Integer id, @RequestBody UpdatePriceRuleRequest req, HttpServletRequest request) {
        PriceRuleResponse resp = priceRuleService.updatePriceRule(id, req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "PriceRule updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // delete a price rule (owner/admin)
    @DeleteMapping("/api/pricerules/{id}")
    public ResponseEntity<DataResponse> deletePriceRule(@PathVariable Integer id, HttpServletRequest request) {
        String resp = priceRuleService.deletePriceRule(id, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(null, resp, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // list price rules by field (public)
    @GetMapping("/api/fields/{fieldId}/pricerules")
    public ResponseEntity<DataResponse> listByField(@PathVariable Integer fieldId) {
        List<PriceRuleResponse> list = priceRuleService.listByField(fieldId);
        DataResponse body = DataResponse.success(list, "Price rules fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}
