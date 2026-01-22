package com.badminton.shop.ws_booking_sport.venue.repository;

import com.badminton.shop.ws_booking_sport.model.action.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    Optional<Favorite> findByUserIdAndVenueId(Integer userId, Integer venueId);
    List<Favorite> findByUserId(Integer userId);
    void deleteByUserIdAndVenueId(Integer userId, Integer venueId);
}

