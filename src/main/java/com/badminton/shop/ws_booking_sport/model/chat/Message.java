package com.badminton.shop.ws_booking_sport.model.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    private Integer senderId; // was String
    private String senderRole;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;
}
