// ...existing code...
package com.badminton.shop.ws_booking_sport.core.repository;

import com.badminton.shop.ws_booking_sport.model.core.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);

    // find account by its linked user's id
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId")
    Optional<Account> findByUserId(@Param("userId") Integer userId);
}
