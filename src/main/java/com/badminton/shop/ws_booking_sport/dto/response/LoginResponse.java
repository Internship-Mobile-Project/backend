package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String refreshToken;

    public LoginResponse() {}

    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public LoginResponse(String accessToken) {
        this.accessToken = accessToken;
    }

}
