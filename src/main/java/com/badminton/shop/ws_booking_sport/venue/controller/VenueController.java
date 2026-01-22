package com.badminton.shop.ws_booking_sport.venue.controller;

import com.badminton.shop.ws_booking_sport.dto.request.AddVenueRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateVenueRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateMainImageRequest;
import com.badminton.shop.ws_booking_sport.dto.request.CreateFieldRequest;
import com.badminton.shop.ws_booking_sport.dto.response.VenueResponse;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.dto.response.FieldResponse;
import com.badminton.shop.ws_booking_sport.dto.response.VenueDetailResponse;
import com.badminton.shop.ws_booking_sport.venue.service.VenueService;
import com.badminton.shop.ws_booking_sport.dto.response.ReviewResponse;
import com.badminton.shop.ws_booking_sport.security.JwtService;
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
    private final JwtService jwtService; // Need this to extract user ID for favorites

    @PostMapping
    public ResponseEntity<DataResponse> createVenue(@RequestBody AddVenueRequest req, HttpServletRequest request) {
        VenueResponse resp = venueService.createVenue(req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Venue created", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse> getVenueDetail(@PathVariable Integer id) {
        VenueDetailResponse resp = venueService.getVenueDetail(id);
        DataResponse body = DataResponse.success(resp, "Venue detail fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/sport/{sportName}")
    public ResponseEntity<DataResponse> getVenuesBySport(@PathVariable String sportName) {
        List<VenueResponse> resp = venueService.getVenuesBySport(sportName);
        DataResponse body = DataResponse.success(resp, "Venues fetched by sport", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse> updateVenue(@PathVariable Integer id, @RequestBody UpdateVenueRequest req, HttpServletRequest request) {
        VenueResponse resp = venueService.updateVenue(id, req, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Venue updated", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // batch add fields to venue (owner/admin)
    @PostMapping("/{id}/fields/batch")
    public ResponseEntity<DataResponse> addFields(@PathVariable("id") Integer venueId, @RequestBody List<CreateFieldRequest> reqs, HttpServletRequest request) {
        List<FieldResponse> resp = venueService.addFields(venueId, reqs, request.getHeader("Authorization"));
        DataResponse body = DataResponse.success(resp, "Fields added", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
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

    // NEW endpoint: get venues for the authenticated owner based on the Authorization header
    @GetMapping("/owner")
    public ResponseEntity<DataResponse> listByAuthenticatedOwner(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        List<VenueResponse> list = venueService.listByOwner(auth);
        DataResponse body = DataResponse.success(list, "Venues fetched for authenticated owner", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // new endpoint: latest 5 reviews for a venue
    @GetMapping("/{id}/reviews/latest")
    public ResponseEntity<DataResponse> latestReviews(@PathVariable("id") Integer venueId) {
        List<ReviewResponse> list = venueService.getLatestReviews(venueId);
        DataResponse body = DataResponse.success(list, "Latest reviews fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // new endpoint: paginated reviews for a venue (page, size)
    @GetMapping("/{id}/reviews")
    public ResponseEntity<DataResponse> paginatedReviews(@PathVariable("id") Integer venueId,
                                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                                         @RequestParam(value = "size", defaultValue = "10") int size) {
        var pageResp = venueService.getReviewsPaginated(venueId, page, size);
        DataResponse body = DataResponse.success(pageResp, "Reviews fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // LIST FAVORITE VENUES
    @GetMapping("/favorites")
    public ResponseEntity<DataResponse> getFavorites(HttpServletRequest request) {
        Integer userId = extractUserId(request);
        List<VenueResponse> list = venueService.getFavoriteVenues(userId);
        return ResponseEntity.ok(DataResponse.success(list, "Favorite venues fetched", HttpStatus.OK.value()));
    }

    // ADD FAVORITE
    @PostMapping("/{id}/favorite")
    public ResponseEntity<DataResponse> addFavorite(@PathVariable("id") Integer venueId, HttpServletRequest request) {
        Integer userId = extractUserId(request);
        venueService.addFavorite(userId, venueId);
        return ResponseEntity.ok(DataResponse.success(null, "Venue added to favorites", HttpStatus.OK.value()));
    }

    // REMOVE FAVORITE
    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<DataResponse> removeFavorite(@PathVariable("id") Integer venueId, HttpServletRequest request) {
        Integer userId = extractUserId(request);
        venueService.removeFavorite(userId, venueId);
        return ResponseEntity.ok(DataResponse.success(null, "Venue removed from favorites", HttpStatus.OK.value()));
    }

    private Integer extractUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        String token = auth.substring(7);
        String subject = jwtService.extractSubject(token); // subject is userId
        try {
            return Integer.parseInt(subject);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID in token");
        }
    }
}
