package com.badminton.shop.ws_booking_sport.booking.repository;

import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    // Find bookings by customer ID (user ID), ordered by newest first
    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);
}
