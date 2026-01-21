package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;

@Data
public class CreateMomoOrderRequest {
    private String amount;
    private String orderInfo;
    private String orderId; // optional, or backend can generate
    private String returnUrl; // optional override
}

