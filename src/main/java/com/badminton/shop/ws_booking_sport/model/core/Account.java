package com.badminton.shop.ws_booking_sport.model.core;

import com.badminton.shop.ws_booking_sport.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private String email;

    private String password;
    private boolean verified;
    private LocalDateTime lastLogin;
    private String verifyCode;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime verifyCodeExpiry;
}
