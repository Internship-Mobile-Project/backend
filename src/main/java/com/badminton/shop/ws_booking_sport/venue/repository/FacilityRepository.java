package com.badminton.shop.ws_booking_sport.venue.repository;

import com.badminton.shop.ws_booking_sport.model.venue.Facility;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Integer> {
    List<Facility> findByVenueId(Integer venueId);
    List<Facility> findByVenue(Venue venue);
}

