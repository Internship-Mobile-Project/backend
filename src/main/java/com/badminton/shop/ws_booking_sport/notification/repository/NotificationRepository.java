package com.badminton.shop.ws_booking_sport.notification.repository;

import com.badminton.shop.ws_booking_sport.enums.NotificationStatus;
import com.badminton.shop.ws_booking_sport.model.action.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserRecipientOrderByCreatedAtDesc(String userRecipient);
    List<Notification> findByUserRecipientAndStatusOrderByCreatedAtDesc(String userRecipient, NotificationStatus status);
}

