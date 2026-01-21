package com.badminton.shop.ws_booking_sport.model.core;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String phone;
    private String avatarUrl;
    private String backgroundUrl;
    private boolean active;

    @Embedded
    private Address address;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
