package com.badminton.shop.ws_booking_sport.venue.service;

import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.dto.request.CreatePriceRuleRequest;
import com.badminton.shop.ws_booking_sport.dto.request.UpdatePriceRuleRequest;
import com.badminton.shop.ws_booking_sport.dto.response.PriceRuleResponse;
import com.badminton.shop.ws_booking_sport.model.core.Account;
import com.badminton.shop.ws_booking_sport.model.venue.Field;
import com.badminton.shop.ws_booking_sport.model.venue.PriceRule;
import com.badminton.shop.ws_booking_sport.venue.repository.FieldRepository;
import com.badminton.shop.ws_booking_sport.venue.repository.PriceRuleRepository;
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
public class PriceRuleService {

    private final PriceRuleRepository priceRuleRepository;
    private final FieldRepository fieldRepository;
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
    public PriceRuleResponse createPriceRule(Integer fieldId, CreatePriceRuleRequest req, String authorizationHeader) {
        if (req == null) throw new IllegalArgumentException("Request body is required");
        Account account = validateOwner(authorizationHeader);
        Field field = fieldRepository.findById(fieldId).orElseThrow(() -> new IllegalArgumentException("Field not found"));

        if (account.getRole() != Role.ADMIN) {
            if (field.getVenue() == null || field.getVenue().getOwner() == null || !field.getVenue().getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to add price rule to this field");
            }
        }

        // basic validation
        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        PriceRule pr = new PriceRule();
        pr.setDayOfWeek(req.getDayOfWeek());
        pr.setStartTime(req.getStartTime());
        pr.setEndTime(req.getEndTime());
        pr.setPricePerHour(req.getPricePerHour());
        pr.setField(field);

        PriceRule saved = priceRuleRepository.save(pr);
        return toResponse(saved);
    }

    // New: add multiple price rules to a field (batch)
    @Transactional
    public List<PriceRuleResponse> addPriceRules(Integer fieldId, List<CreatePriceRuleRequest> reqs, String authorizationHeader) {
        if (reqs == null || reqs.isEmpty()) throw new IllegalArgumentException("Request list is empty");
        Account account = validateOwner(authorizationHeader);
        Field field = fieldRepository.findById(fieldId).orElseThrow(() -> new IllegalArgumentException("Field not found"));

        if (account.getRole() != Role.ADMIN) {
            if (field.getVenue() == null || field.getVenue().getOwner() == null || !field.getVenue().getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to add price rules to this field");
            }
        }

        List<PriceRule> toSave = reqs.stream().map(req -> {
            if (req.getStartTime() == null || req.getEndTime() == null) {
                throw new IllegalArgumentException("startTime and endTime are required for each price rule");
            }
            if (req.getEndTime().isBefore(req.getStartTime())) {
                throw new IllegalArgumentException("endTime must be after startTime for each price rule");
            }
            PriceRule pr = new PriceRule();
            pr.setDayOfWeek(req.getDayOfWeek());
            pr.setStartTime(req.getStartTime());
            pr.setEndTime(req.getEndTime());
            pr.setPricePerHour(req.getPricePerHour());
            pr.setField(field);
            return pr;
        }).collect(Collectors.toList());

        List<PriceRule> saved = priceRuleRepository.saveAll(toSave);
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PriceRuleResponse getPriceRule(Integer id) {
        PriceRule pr = priceRuleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("PriceRule not found"));
        return toResponse(pr);
    }

    @Transactional
    public PriceRuleResponse updatePriceRule(Integer id, UpdatePriceRuleRequest req, String authorizationHeader) {
        if (req == null) throw new IllegalArgumentException("Request body is required");
        Account account = validateOwner(authorizationHeader);
        PriceRule pr = priceRuleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("PriceRule not found"));
        Field field = pr.getField();

        if (account.getRole() != Role.ADMIN) {
            if (field == null || field.getVenue() == null || field.getVenue().getOwner() == null || !field.getVenue().getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to update this price rule");
            }
        }

        if (req.getDayOfWeek() != null) pr.setDayOfWeek(req.getDayOfWeek());
        if (req.getStartTime() != null) pr.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) pr.setEndTime(req.getEndTime());
        if (req.getPricePerHour() != null) pr.setPricePerHour(req.getPricePerHour());

        // additional validation if both times present
        if (pr.getStartTime() != null && pr.getEndTime() != null && pr.getEndTime().isBefore(pr.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        PriceRule saved = priceRuleRepository.save(pr);
        return toResponse(saved);
    }

    @Transactional
    public String deletePriceRule(Integer id, String authorizationHeader) {
        Account account = validateOwner(authorizationHeader);
        PriceRule pr = priceRuleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("PriceRule not found"));
        Field field = pr.getField();

        if (account.getRole() != Role.ADMIN) {
            if (field == null || field.getVenue() == null || field.getVenue().getOwner() == null || !field.getVenue().getOwner().getId().equals(account.getUser().getId())) {
                throw new AccessDeniedException("You are not allowed to delete this price rule");
            }
        }

        priceRuleRepository.delete(pr);
        return "PriceRule deleted";
    }

    public List<PriceRuleResponse> listByField(Integer fieldId) {
        return priceRuleRepository.findByFieldId(fieldId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private PriceRuleResponse toResponse(PriceRule pr) {
        PriceRuleResponse r = new PriceRuleResponse();
        r.setId(pr.getId());
        r.setDayOfWeek(pr.getDayOfWeek());
        r.setStartTime(pr.getStartTime());
        r.setEndTime(pr.getEndTime());
        r.setPricePerHour(pr.getPricePerHour());
        r.setFieldId(pr.getField() != null ? pr.getField().getId() : null);
        return r;
    }
}
