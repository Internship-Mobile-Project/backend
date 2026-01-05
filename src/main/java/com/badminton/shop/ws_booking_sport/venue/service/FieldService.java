package com.badminton.shop.ws_booking_sport.venue.service;

import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.dto.request.CreateFieldRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdateFieldRequest;
import com.badminton.shop.ws_booking_sport.dto.request.CreatePriceRuleRequest;
import com.badminton.shop.ws_booking_sport.dto.response.FieldResponse;
import com.badminton.shop.ws_booking_sport.dto.response.PriceRuleResponse;
import com.badminton.shop.ws_booking_sport.model.core.Account;
import com.badminton.shop.ws_booking_sport.model.venue.Field;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.badminton.shop.ws_booking_sport.venue.repository.FieldRepository;
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
public class FieldService {

    private final FieldRepository fieldRepository;
    private final VenueRepository venueRepository;
    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final PriceRuleService priceRuleService;

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
    public FieldResponse createField(Integer venueId, CreateFieldRequest req, String authorizationHeader) {
        if (req == null) throw new IllegalArgumentException("Request body is required");
        Account account = validateOwner(authorizationHeader);
        Venue venue = venueRepository.findById(venueId).orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        if (account.getRole() != Role.ADMIN) {
            if (venue.getOwner() == null || !venue.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to add field to this venue");
            }
        }

        Field f = new Field();
        f.setName(req.getName());
        f.setType(req.getType());
        f.setVenue(venue);

        Field saved = fieldRepository.save(f);
        return toResponse(saved);
    }

    public FieldResponse getField(Integer id) {
        Field f = fieldRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Field not found"));
        return toResponse(f);
    }

    @Transactional
    public FieldResponse updateField(Integer id, UpdateFieldRequest req, String authorizationHeader) {
        if (req == null) throw new IllegalArgumentException("Request body is required");
        Account account = validateOwner(authorizationHeader);
        Field f = fieldRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Field not found"));
        Venue venue = f.getVenue();

        if (account.getRole() != Role.ADMIN) {
            if (venue == null || !venue.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to update this field");
            }
        }

        if (req.getName() != null) f.setName(req.getName());
        if (req.getType() != null) f.setType(req.getType());

        Field saved = fieldRepository.save(f);
        return toResponse(saved);
    }

    @Transactional
    public String deleteField(Integer id, String authorizationHeader) {
        Account account = validateOwner(authorizationHeader);
        Field f = fieldRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Field not found"));
        Venue venue = f.getVenue();

        if (account.getRole() != Role.ADMIN) {
            if (venue == null || !venue.getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to delete this field");
            }
        }

        fieldRepository.delete(f);
        return "Field deleted";
    }

    public List<FieldResponse> listByVenue(Integer venueId) {
        return fieldRepository.findByVenueId(venueId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // New: Add multiple price rules to a field via FieldService delegating to PriceRuleService
    @Transactional
    public List<PriceRuleResponse> addPriceRules(Integer fieldId, List<CreatePriceRuleRequest> reqs, String authorizationHeader) {
        // reuse validation logic in PriceRuleService (PriceRuleService will validate permissions)
        return priceRuleService.addPriceRules(fieldId, reqs, authorizationHeader);
    }

    private FieldResponse toResponse(Field f) {
        FieldResponse r = new FieldResponse();
        r.setId(f.getId());
        r.setName(f.getName());
        r.setType(f.getType());
        r.setVenueId(f.getVenue() != null ? f.getVenue().getId() : null);
        // include price rules for this field
        if (f.getId() != null) {
            try {
                r.setPriceRules(priceRuleService.listByField(f.getId()));
            } catch (Exception e) {
                // in case of any issue fetching rules, set empty list to avoid breaking response
                r.setPriceRules(java.util.Collections.emptyList());
            }
        } else {
            r.setPriceRules(java.util.Collections.emptyList());
        }
        return r;
    }
}
