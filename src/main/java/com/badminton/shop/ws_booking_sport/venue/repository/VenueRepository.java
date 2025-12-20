package com.badminton.shop.ws_booking_sport.venue.repository;

import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Integer> {
    List<Venue> findByOwnerId(Integer ownerId);
}

