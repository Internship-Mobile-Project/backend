package com.badminton.shop.ws_booking_sport.booking.repository;

import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
}

