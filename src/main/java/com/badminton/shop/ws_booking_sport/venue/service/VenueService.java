package com.badminton.shop.ws_booking_sport.venue.service;

import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.dto.request.AddVenueRequest;
import com.badminton.shop.ws_booking_sport.dto.request.AddressRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateVenueRequest;
import com.badminton.shop.ws_booking_sport.dto.request.CreateFieldRequest;
import com.badminton.shop.ws_booking_sport.dto.response.VenueResponse;
import com.badminton.shop.ws_booking_sport.dto.response.FieldResponse;
import com.badminton.shop.ws_booking_sport.dto.response.VenueDetailResponse;
import com.badminton.shop.ws_booking_sport.goong.GoongMapService;
import com.badminton.shop.ws_booking_sport.goong.GoongResponse;
import com.badminton.shop.ws_booking_sport.model.core.Account;
import com.badminton.shop.ws_booking_sport.model.core.Address;
import com.badminton.shop.ws_booking_sport.model.core.Owner;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.badminton.shop.ws_booking_sport.model.venue.Field;
import com.badminton.shop.ws_booking_sport.venue.repository.VenueRepository;
import com.badminton.shop.ws_booking_sport.venue.repository.FieldRepository;
import com.badminton.shop.ws_booking_sport.security.JwtService;
import com.badminton.shop.ws_booking_sport.enums.Role;
import com.badminton.shop.ws_booking_sport.booking.repository.ReviewRepository;
import com.badminton.shop.ws_booking_sport.dto.response.ReviewResponse;
import com.badminton.shop.ws_booking_sport.model.booking.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final FieldRepository fieldRepository;
    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final GoongMapService goongMapService;
    private final ReviewRepository reviewRepository;
    private final FieldService fieldService;
    private final FacilityService facilityService;

    private Account validateOwner(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token does not contain email");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getRole() != Role.OWNER && account.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only owners or admins are allowed to perform this action");
        }

        return account;
    }

    @Transactional
    public VenueResponse createVenue(AddVenueRequest req, String authorizationHeader) {
        if (req == null) throw new IllegalArgumentException("Request body is required");

        Account account = validateOwner(authorizationHeader);
        if (!(account.getUser() instanceof Owner)) {
            throw new AccessDeniedException("Account is not an owner");
        }

        Owner owner = (Owner) account.getUser();

        Venue v = new Venue();
        v.setName(req.getName());
        v.setDescription(req.getDescription());
        v.setSport(req.getSport());
        v.setTimeOpen(req.getTimeOpen());
        v.setTimeClose(req.getTimeClose());
        v.setOwner(owner);
        v.setCreatedAt(LocalDateTime.now());
        v.setUpdatedAt(LocalDateTime.now());

        AddressRequest addrReq = req.getAddress();
        if (addrReq != null) {
            Address address = new Address(addrReq);
            try {
                GoongResponse.Location loc = goongMapService.getGeoLocation(addrReq.formatAddress());
                if (loc != null) {
                    // convert float -> Double
                    address.setLatitude(Double.valueOf(loc.getLat()));
                    address.setLongitude(Double.valueOf(loc.getLng()));
                }
            } catch (Exception ignored) {
            }
            v.setAddress(address);
        }

        Venue saved = venueRepository.save(v);
        return toResponse(saved);
    }

    // New: add list of fields to a venue (batch)
    @Transactional
    public List<FieldResponse> addFields(Integer venueId, List<CreateFieldRequest> reqs, String authorizationHeader) {
        if (reqs == null || reqs.isEmpty()) throw new IllegalArgumentException("Request list is empty");
        Account account = validateOwner(authorizationHeader);
        Venue v = venueRepository.findById(venueId).orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        if (account.getRole() != Role.ADMIN) {
            if (v.getOwner() == null || !v.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to add fields to this venue");
            }
        }

        List<Field> fields = reqs.stream().map(r -> {
            Field f = new Field();
            f.setName(r.getName());
            f.setType(r.getType());
            f.setVenue(v);
            return f;
        }).collect(Collectors.toList());

        List<Field> saved = fieldRepository.saveAll(fields);

        return saved.stream().map(f -> {
            FieldResponse fr = new FieldResponse();
            fr.setId(f.getId());
            fr.setName(f.getName());
            fr.setType(f.getType());
            fr.setVenueId(v.getId());
            return fr;
        }).collect(Collectors.toList());
    }

    public VenueResponse getVenue(Integer id) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Venue not found"));
        return toResponse(v);
    }

    // NEW: get venue detail
    public VenueDetailResponse getVenueDetail(Integer id) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Venue not found"));
        VenueDetailResponse resp = new VenueDetailResponse();
        resp.setId(v.getId());
        resp.setName(v.getName());
        resp.setDescription(v.getDescription());
        resp.setSport(v.getSport());

        // map address -> AddressRequest
        if (v.getAddress() != null) {
            Address a = v.getAddress();
            AddressRequest ar = new AddressRequest();
            ar.setStreet(a.getStreet());
            ar.setDistrict(a.getDistrict());
            ar.setCity(a.getCity());
            ar.setProvince(a.getProvince());
            resp.setAddress(ar);
        }

        // always set open/close times (may be null)
        resp.setTimeOpen(v.getTimeOpen());
        resp.setTimeClose(v.getTimeClose());

        resp.setImageUrls(v.getImageUrls());
        resp.setMainImageUrl((v.getImageUrls() != null && !v.getImageUrls().isEmpty()) ? v.getImageUrls().get(0) : null);

        // fields with price rules
        try {
            resp.setFields(fieldService.listByVenue(v.getId()));
        } catch (Exception e) {
            resp.setFields(java.util.Collections.emptyList());
        }

        // latest reviews
        try {
            resp.setReviews(getLatestReviews(v.getId()));
        } catch (Exception e) {
            resp.setReviews(java.util.Collections.emptyList());
        }

        // facilities: map using FacilityService
        try {
            resp.setFacilities(facilityService.listByVenue(v.getId()));
        } catch (Exception e) {
            resp.setFacilities(java.util.Collections.emptyList());
        }

        resp.setOwnerId(v.getOwner() != null ? v.getOwner().getId() : null);
        resp.setRating(v.getRating());

        return resp;
    }

    @Transactional
    public VenueResponse updateVenue(Integer id, UpdateVenueRequest req, String authorizationHeader) {
        if (req == null) throw new IllegalArgumentException("Request body is required");

        Account account = validateOwner(authorizationHeader);
        Venue v = venueRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        // allow admin or owner of the venue
        if (account.getRole() != Role.ADMIN) {
            if (v.getOwner() == null || !v.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to update this venue");
            }
        }

        if (req.getName() != null) v.setName(req.getName());
        if (req.getDescription() != null) v.setDescription(req.getDescription());
        if (req.getSport() != null) v.setSport(req.getSport());
        if (req.getTimeOpen() != null) v.setTimeOpen(req.getTimeOpen());
        if (req.getTimeClose() != null) v.setTimeClose(req.getTimeClose());

        AddressRequest addrReq = req.getAddress();
        if (addrReq != null) {
            if (v.getAddress() == null) {
                v.setAddress(new Address(addrReq));
            } else {
                Address a = v.getAddress();
                if (addrReq.getStreet() != null) a.setStreet(addrReq.getStreet());
                if (addrReq.getDistrict() != null) a.setDistrict(addrReq.getDistrict());
                if (addrReq.getCity() != null) a.setCity(addrReq.getCity());
                if (addrReq.getProvince() != null) a.setProvince(addrReq.getProvince());
            }
            try {
                GoongResponse.Location loc = goongMapService.getGeoLocation(addrReq.formatAddress());
                if (loc != null) {
                    v.getAddress().setLatitude(Double.valueOf(loc.getLat()));
                    v.getAddress().setLongitude(Double.valueOf(loc.getLng()));
                }
            } catch (Exception ignored) {
            }
        }

        v.setUpdatedAt(LocalDateTime.now());
        Venue saved = venueRepository.save(v);
        return toResponse(saved);
    }

    @Transactional
    public String deleteVenue(Integer id, String authorizationHeader) {
        Account account = validateOwner(authorizationHeader);
        Venue v = venueRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        // allow admin or owner
        if (account.getRole() != Role.ADMIN) {
            if (v.getOwner() == null || !v.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to delete this venue");
            }
        }

        venueRepository.delete(v);
        return "Venue deleted";
    }

    public List<VenueResponse> listAll() {
        return venueRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<VenueResponse> listByOwner(Integer ownerId) {
        return venueRepository.findByOwnerId(ownerId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // NEW: paginated list of venues
    public Page<VenueResponse> listPaginated(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        Pageable p = PageRequest.of(safePage, safeSize);
        Page<Venue> pr = venueRepository.findAll(p);
        return pr.map(this::toResponse);
    }

    // NEW: list venues for authenticated owner using Authorization header (access token)
    public List<VenueResponse> listByOwner(String authorizationHeader) {
        Account account = validateOwner(authorizationHeader);
        Integer ownerId = account.getUser() != null ? account.getUser().getId() : null;
        if (ownerId == null) throw new IllegalArgumentException("Owner id not found in account");
        return listByOwner(ownerId);
    }

    // Update or set the main image (first image) for a venue
    @Transactional
    public VenueResponse updateMainImage(Integer id, String imageUrl, String authorizationHeader) {
        if (imageUrl == null || imageUrl.isBlank()) throw new IllegalArgumentException("imageUrl is required");
        Account account = validateOwner(authorizationHeader);
        Venue v = venueRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        if (account.getRole() != Role.ADMIN) {
            if (v.getOwner() == null || !v.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to update this venue");
            }
        }

        List<String> imgs = v.getImageUrls();
        if (imgs == null) imgs = new java.util.ArrayList<>();

        // if already present, remove it so we can re-add at front
        imgs.removeIf(s -> s.equals(imageUrl));
        imgs.add(0, imageUrl);

        v.setImageUrls(imgs);
        v.setUpdatedAt(LocalDateTime.now());
        Venue saved = venueRepository.save(v);
        return toResponse(saved);
    }

    // Return the list of images for a venue
    public List<String> listImages(Integer id) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Venue not found"));
        List<String> imgs = v.getImageUrls();
        return imgs == null ? java.util.Collections.emptyList() : imgs;
    }

    // new: get latest 5 reviews for a venue
    public List<ReviewResponse> getLatestReviews(Integer venueId) {
        List<Review> reviews = reviewRepository.findTop5ByBookingFieldVenueIdOrderByCreatedAtDesc(venueId);
        return reviews.stream().map(this::mapReview).collect(Collectors.toList());
    }

    // new: get paginated reviews for a venue
    public org.springframework.data.domain.Page<ReviewResponse> getReviewsPaginated(Integer venueId, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Review> pr = reviewRepository.findByBookingFieldVenueId(venueId, p);
        return pr.map(this::mapReview);
    }

    private ReviewResponse mapReview(Review r) {
        if (r == null) return null;
        ReviewResponse resp = new ReviewResponse();
        resp.setId(r.getId());
        resp.setBookingId(r.getBooking() != null ? r.getBooking().getId() : null);
        resp.setCustomerId(r.getCustomer() != null ? r.getCustomer().getId() : null);
        resp.setCustomerName(r.getCustomer() != null ? r.getCustomer().getName() : null);
        resp.setRating(r.getRating());
        resp.setComment(r.getComment());
        resp.setPhotos(r.getPhotos());
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        return resp;
    }

    private VenueResponse toResponse(Venue v) {
        VenueResponse resp = new VenueResponse();
        resp.setId(v.getId());
        resp.setName(v.getName());
        resp.setDescription(v.getDescription());
        resp.setSport(v.getSport());
        resp.setAddress(v.getAddress());
        resp.setTimeOpen(v.getTimeOpen());
        resp.setTimeClose(v.getTimeClose());
        resp.setImageUrls(v.getImageUrls());
        resp.setOwnerId(v.getOwner() != null ? v.getOwner().getId() : null);
        resp.setRating(v.getRating());
        resp.setCreatedAt(v.getCreatedAt());
        resp.setUpdatedAt(v.getUpdatedAt());
        return resp;
    }
}
