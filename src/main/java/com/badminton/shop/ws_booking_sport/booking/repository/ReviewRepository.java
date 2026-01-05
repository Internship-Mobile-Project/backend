package com.badminton.shop.ws_booking_sport.booking.repository;

import com.badminton.shop.ws_booking_sport.model.booking.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByBookingId(String bookingId);
    List<Review> findByCustomerId(Integer customerId);

    // find latest top 5 reviews for a venue (via booking -> slot -> field -> venue relationship)
    List<Review> findTop5ByBookingFieldVenueIdOrderByCreatedAtDesc(Integer venueId);

    // paginated query for reviews of a venue
    Page<Review> findByBookingFieldVenueId(Integer venueId, Pageable pageable);
}
