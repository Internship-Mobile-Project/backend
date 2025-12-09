package com.badminton.shop.ws_booking_sport.model.chat;

import com.badminton.shop.ws_booking_sport.enums.RoomStatus;
import com.badminton.shop.ws_booking_sport.model.core.Admin;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = true)
    private Admin admin;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<Message> messages;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

