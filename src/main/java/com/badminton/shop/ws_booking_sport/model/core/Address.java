package com.badminton.shop.ws_booking_sport.model.core;

import jakarta.persistence.Embeddable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address {
    private String street;
    private String district;
    private String province;
    private String country;
    private float latitude;
    private float longitude;
}
