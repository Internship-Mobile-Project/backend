package com.badminton.shop.ws_booking_sport.model.core;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String phone;
    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;
    private String backgroundUrl;
    private boolean active;

    @Embedded
    private Address address;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
