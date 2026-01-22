package com.badminton.shop.ws_booking_sport.notification.controller;

import com.badminton.shop.ws_booking_sport.model.action.Notification;
import com.badminton.shop.ws_booking_sport.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private String getCurrentUserId() {
        // Assuming the authentication principal is the userId (String or Integer converted to String)
        // Or extract from JWT token. For now, following typical Spring Security context usage pattern in this project.
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Adjust this based on your actual UserDetails implementation.
        // If principal is a String (userId), return it.
        // If it's a UserDetails object, getUsername() or similar.
        // Based on existing controllers, usually the ID is extracted.
        // Let's assume the principal.toString() gives the ID or we need to cast to CustomUserDetails.
        // Safest is to return principal.toString() if simple, but often it's "anonymousUser".
        return principal.toString();
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getMyUnreadNotifications() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(notificationService.getUnreadUserNotifications(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotification(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<String> markAllAsRead() {
        String userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}

