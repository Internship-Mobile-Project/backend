package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Integer id;
    private String chatRoomId;
    private Integer senderId;
    private String senderRole;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;
}

