package com.badminton.shop.ws_booking_sport.dto.request;

import com.badminton.shop.ws_booking_sport.model.core.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String name;
    private String phone;
    private String avatarUrl;
    private AddressRequest addressRequest;
}

