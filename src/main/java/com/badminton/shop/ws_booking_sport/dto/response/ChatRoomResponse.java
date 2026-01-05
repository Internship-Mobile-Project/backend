package com.badminton.shop.ws_booking_sport.dto.response;

import com.badminton.shop.ws_booking_sport.enums.ChatStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private String id;
    private String userId;
    private String ownerId;
    private ChatStatus status;
    private List<MessageResponse> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

