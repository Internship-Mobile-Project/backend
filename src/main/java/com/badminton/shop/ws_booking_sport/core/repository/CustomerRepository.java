package com.badminton.shop.ws_booking_sport.core.repository;

import com.badminton.shop.ws_booking_sport.model.core.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}

