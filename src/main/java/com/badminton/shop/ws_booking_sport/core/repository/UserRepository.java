package com.badminton.shop.ws_booking_sport.core.repository;

import com.badminton.shop.ws_booking_sport.dto.response.ChartDataPoint;
import com.badminton.shop.ws_booking_sport.model.core.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query(value = "SELECT DATE(u.created_at) as date, COUNT(*) as value " +
            "FROM users u " + // Tên bảng trong DB là 'users'
            "WHERE u.created_at >= :startDate " +
            "GROUP BY DATE(u.created_at) " +
            "ORDER BY DATE(u.created_at) ASC", nativeQuery = true)
    List<Object[]> getUserGrowthStats(@Param("startDate") LocalDateTime startDate);
}