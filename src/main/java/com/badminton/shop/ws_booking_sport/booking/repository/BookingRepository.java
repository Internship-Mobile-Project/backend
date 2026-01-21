package com.badminton.shop.ws_booking_sport.booking.repository;

import com.badminton.shop.ws_booking_sport.dto.response.ChartDataPoint;
import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    @Query(value = "SELECT DATE(b.booked_at) as date, COUNT(*) as value " +
            "FROM booking b " + // Tên bảng là 'booking'
            "WHERE b.booked_at >= :startDate " +
            "GROUP BY DATE(b.booked_at) " +
            "ORDER BY DATE(b.booked_at) ASC", nativeQuery = true)
    List<Object[]> getBookingStats(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT DATE(b.booked_at) as date, SUM(b.total_price) as value " +
            "FROM booking b " +
            "WHERE b.booked_at >= :startDate " +
            "GROUP BY DATE(b.booked_at) " +
            "ORDER BY DATE(b.booked_at) ASC", nativeQuery = true)
    List<Object[]> getRevenueStats(@Param("startDate") LocalDateTime startDate);
}