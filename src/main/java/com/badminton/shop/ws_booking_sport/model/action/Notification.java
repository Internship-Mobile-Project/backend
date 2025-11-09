package com.badminton.shop.ws_booking_sport.model.action;

import com.badminton.shop.ws_booking_sport.enums.NotificationStatus;
import com.badminton.shop.ws_booking_sport.enums.NotificationType;
import com.badminton.shop.ws_booking_sport.enums.RecipientType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userRecipient;

    @Enumerated(EnumType.STRING)
    private RecipientType recipientType;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String title;
    private String message;
    private String referenceType;
    private String referenceId;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private boolean isPush;
}

