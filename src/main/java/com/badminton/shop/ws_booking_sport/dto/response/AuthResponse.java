// ...existing code...
package com.badminton.shop.ws_booking_sport.dto.response;

import com.badminton.shop.ws_booking_sport.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private int id;
    private String name;
    private String email;
    private String accessToken;
    private String refreshToken;
    private Role role;
    // optional: when an owner logs in, include their primary venue id for the client
    private Integer venueId;
}
