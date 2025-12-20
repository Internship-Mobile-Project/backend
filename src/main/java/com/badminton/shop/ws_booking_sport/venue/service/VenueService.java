package com.badminton.shop.ws_booking_sport.venue.service;

import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.dto.request.AddVenueRequest;
import com.badminton.shop.ws_booking_sport.dto.request.AddressRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateVenueRequest;
import com.badminton.shop.ws_booking_sport.dto.response.VenueResponse;
import com.badminton.shop.ws_booking_sport.goong.GoongMapService;
import com.badminton.shop.ws_booking_sport.goong.GoongResponse;
import com.badminton.shop.ws_booking_sport.model.core.Account;
import com.badminton.shop.ws_booking_sport.model.core.Address;
import com.badminton.shop.ws_booking_sport.model.core.Owner;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.badminton.shop.ws_booking_sport.venue.repository.VenueRepository;
import com.badminton.shop.ws_booking_sport.security.JwtService;
import com.badminton.shop.ws_booking_sport.enums.Role;
import lombok.RequiredArgsConstructor;
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
    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final GoongMapService goongMapService;

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

    public VenueResponse getVenue(Integer id) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Venue not found"));
        return toResponse(v);
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
