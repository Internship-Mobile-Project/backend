package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataRequest {
    private String message;             // short description or action message
    private Map<String, Object> data;   // main request payload
    private int code;                   // optional status or type code
}
