package com.badminton.shop.ws_booking_sport.notification.service;

import com.badminton.shop.ws_booking_sport.enums.NotificationStatus;
import com.badminton.shop.ws_booking_sport.enums.NotificationType;
import com.badminton.shop.ws_booking_sport.enums.RecipientType;
import com.badminton.shop.ws_booking_sport.model.action.Notification;
import com.badminton.shop.ws_booking_sport.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserRecipientOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadUserNotifications(String userId) {
        return notificationRepository.findByUserRecipientAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);
    }

    public Notification getNotificationById(String id) {
        return notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("Notification not found"));
    }

    @Transactional
    public Notification createNotification(String recipientId, RecipientType recipientType, NotificationType type, String title, String message, String refType, String refId) {
        Notification n = new Notification();
        n.setUserRecipient(recipientId);
        n.setRecipientType(recipientType);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setReferenceType(refType);
        n.setReferenceId(refId);
        n.setStatus(NotificationStatus.UNREAD);
        n.setCreatedAt(LocalDateTime.now());
        n.setPush(false); // Default, can be updated later if push logic is added

        return notificationRepository.save(n);
    }

    @Transactional
    public Notification markAsRead(String id) {
        Notification n = getNotificationById(id);
        n.setStatus(NotificationStatus.READ);
        n.setReadAt(LocalDateTime.now());
        return notificationRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserRecipientAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setStatus(NotificationStatus.READ);
            n.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(String id) {
        notificationRepository.deleteById(id);
    }
}

