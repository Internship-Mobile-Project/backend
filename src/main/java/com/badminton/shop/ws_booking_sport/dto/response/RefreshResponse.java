// ...existing code...
package com.badminton.shop.ws_booking_sport.dto.response;

import com.badminton.shop.ws_booking_sport.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {
    private String accessToken;
    private String refreshToken;
    private Role role;
}

