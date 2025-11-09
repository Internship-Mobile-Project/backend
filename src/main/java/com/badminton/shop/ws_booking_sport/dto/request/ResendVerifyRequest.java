// ...existing code...
package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendVerifyRequest {
    private String email;
}

