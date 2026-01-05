package com.badminton.shop.ws_booking_sport.venue.repository;

import com.badminton.shop.ws_booking_sport.model.venue.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<Field, Integer> {
    List<Field> findByVenueId(Integer venueId);
}

