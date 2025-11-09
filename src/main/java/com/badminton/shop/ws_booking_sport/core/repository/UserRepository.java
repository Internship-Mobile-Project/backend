package com.badminton.shop.ws_booking_sport.core.repository;

import com.badminton.shop.ws_booking_sport.model.core.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
}
