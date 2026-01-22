package com.badminton.shop.ws_booking_sport.venue.service;

import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.dto.request.CreateFacilityRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateFacilityRequest;
import com.badminton.shop.ws_booking_sport.dto.response.FacilityResponse;
import com.badminton.shop.ws_booking_sport.model.core.Account;
import com.badminton.shop.ws_booking_sport.model.venue.Facility;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.badminton.shop.ws_booking_sport.venue.repository.FacilityRepository;
import com.badminton.shop.ws_booking_sport.venue.repository.VenueRepository;
import com.badminton.shop.ws_booking_sport.security.JwtService;
import com.badminton.shop.ws_booking_sport.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository facilityRepository;
    private final VenueRepository venueRepository;
    private final JwtService jwtService;
    private final AccountRepository accountRepository;

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
    @Override
    public FacilityResponse create(Integer venueId, CreateFacilityRequest req, String authHeader) {
        if (req == null) throw new IllegalArgumentException("Request body is required");
        Account account = validateOwner(authHeader);
        Venue v = venueRepository.findById(venueId).orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        if (account.getRole() != Role.ADMIN) {
            if (v.getOwner() == null || !v.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to add facilities to this venue");
            }
        }

        Facility f = new Facility();
        f.setName(req.getName());
        f.setDescription(req.getDescription());
        f.setPrice(req.getPrice() == null ? 0.0 : req.getPrice());
        f.setVenue(v);

        Facility saved = facilityRepository.save(f);
        return toResponse(saved);
    }

    @Override
    public List<FacilityResponse> listByVenue(Integer venueId) {
        List<Facility> list = facilityRepository.findByVenueId(venueId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public FacilityResponse getById(Integer id) {
        Facility f = facilityRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        return toResponse(f);
    }

    @Transactional
    @Override
    public FacilityResponse update(Integer id, UpdateFacilityRequest req, String authHeader) {
        if (req == null) throw new IllegalArgumentException("Request body is required");
        Account account = validateOwner(authHeader);
        Facility f = facilityRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        Venue v = f.getVenue();

        if (account.getRole() != Role.ADMIN) {
            if (v == null || v.getOwner() == null || !v.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to update this facility");
            }
        }

        if (req.getName() != null) f.setName(req.getName());
        if (req.getDescription() != null) f.setDescription(req.getDescription());
        if (req.getPrice() != null) f.setPrice(req.getPrice());

        Facility saved = facilityRepository.save(f);
        return toResponse(saved);
    }

    @Transactional
    @Override
    public String delete(Integer id, String authHeader) {
        Account account = validateOwner(authHeader);
        Facility f = facilityRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        Venue v = f.getVenue();

        if (account.getRole() != Role.ADMIN) {
            if (v == null || v.getOwner() == null || !v.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to delete this facility");
            }
        }

        facilityRepository.delete(f);
        return "Facility deleted";
    }

    private FacilityResponse toResponse(Facility f) {
        FacilityResponse r = new FacilityResponse();
        r.setId(f.getId());
        r.setName(f.getName());
        r.setDescription(f.getDescription());
        r.setPrice(f.getPrice());
        return r;
    }
}
