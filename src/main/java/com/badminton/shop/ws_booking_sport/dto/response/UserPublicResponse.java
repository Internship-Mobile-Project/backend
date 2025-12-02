package com.badminton.shop.ws_booking_sport.dto.response;

import com.badminton.shop.ws_booking_sport.model.core.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicResponse {
    private Integer id;
    private String name;
    private String avatarUrl;
    private String backgroundUrl;
    private boolean active;
    private Address address;
    private Float rating; // null if not an owner
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

