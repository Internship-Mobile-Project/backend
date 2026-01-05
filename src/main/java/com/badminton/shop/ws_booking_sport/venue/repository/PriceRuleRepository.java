package com.badminton.shop.ws_booking_sport.venue.repository;

import com.badminton.shop.ws_booking_sport.model.venue.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRuleRepository extends JpaRepository<PriceRule, Integer> {
    List<PriceRule> findByFieldId(Integer fieldId);
}

