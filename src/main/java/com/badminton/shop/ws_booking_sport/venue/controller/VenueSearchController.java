package com.badminton.shop.ws_booking_sport.venue.controller;

import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.dto.response.VenuesResponse;
import com.badminton.shop.ws_booking_sport.model.action.AreaSearchFilter;
import com.badminton.shop.ws_booking_sport.model.action.LocationSearchFilter;
import com.badminton.shop.ws_booking_sport.model.action.SearchFilter;
import com.badminton.shop.ws_booking_sport.venue.service.VenueSearchService;
import com.badminton.shop.ws_booking_sport.venue.service.VenueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueSearchController {
    private final VenueSearchService venueSearchService;
    private final VenueService venueService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(@RequestBody Map<String, Object> body,
                                    @RequestParam(value = "debug", required = false, defaultValue = "false") boolean debug) {
        // decide which filter type to build
        SearchFilter filter;
        if (body.containsKey("latitude") || body.containsKey("longitude")) {
            filter = objectMapper.convertValue(body, LocationSearchFilter.class);
        } else if (body.containsKey("city") || body.containsKey("district")) {
            filter = objectMapper.convertValue(body, AreaSearchFilter.class);
        } else {
            // fallback to base SearchFilter
            filter = objectMapper.convertValue(body, SearchFilter.class);
        }

        if (debug) {
            Map<String, Object> debugResp = venueSearchService.searchDebug(filter);
            return ResponseEntity.ok(debugResp);
        }

        List<VenuesResponse> results = venueSearchService.search(filter);
        return ResponseEntity.ok(results);
    }

    // New: paginated list of venues
    // NOTE: Accept `page` as 1-based for FE convenience. Defaults: page=1, size=10
    @GetMapping
    public ResponseEntity<DataResponse> listPaginated(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        // Convert FE-friendly 1-based page -> service 0-based page index
        int pageIndex = Math.max(1, page) - 1;
        int safeSize = Math.max(1, size);

        var paged = venueService.listPaginated(pageIndex, safeSize);

        // If requested page is out of range but there are results, return last page instead of empty
        if ((paged.getContent() == null || paged.getContent().isEmpty()) && paged.getTotalElements() > 0 && pageIndex > 0) {
            int lastPage = Math.max(0, paged.getTotalPages() - 1);
            paged = venueService.listPaginated(lastPage, safeSize);
        }

        DataResponse body = DataResponse.success(paged, "Venues fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}
