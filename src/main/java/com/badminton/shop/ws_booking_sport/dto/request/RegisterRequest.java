package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {
    private String email;
    private String password;
    private Integer role; // 1=guest,2=owner,3=admin
}

