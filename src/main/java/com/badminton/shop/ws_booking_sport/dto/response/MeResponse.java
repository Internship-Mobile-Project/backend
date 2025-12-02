package com.badminton.shop.ws_booking_sport.dto.response;

import com.badminton.shop.ws_booking_sport.enums.Role;
import com.badminton.shop.ws_booking_sport.model.core.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private String backgroundUrl;
    private boolean active;
    private Address address;
    private Role role;
    private boolean verified;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

