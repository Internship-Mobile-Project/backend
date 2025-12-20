package com.badminton.shop.ws_booking_sport.model.core;

import com.badminton.shop.ws_booking_sport.dto.request.AddressRequest;
import jakarta.persistence.Embeddable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address {
    private String street;
    private String district;
    private String city;
    private String province;
    private Double latitude;
    private Double longitude;

    // convenience constructor from AddressRequest
    public Address(AddressRequest addressRequest) {
        if (addressRequest == null) return;
        this.street = addressRequest.getStreet();
        this.district = addressRequest.getDistrict();
        this.city = addressRequest.getCity();
        this.province = addressRequest.getProvince();
    }
}
