// ...existing code...
package com.badminton.shop.ws_booking_sport.core.repository;

import com.badminton.shop.ws_booking_sport.model.core.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
}

