package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;

@Data
public class AddressRequest {
    private String street;
    private String district;
    private String city;
    private String province;
    public String formatAddress() {;
        StringBuilder sb = new StringBuilder();
        if (street != null && !street.isBlank()) sb.append(street.trim());
        if (district != null && !district.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(district.trim());
        }
        if (city != null && !city.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city.trim());
        }
        if (province != null && !province.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(province.trim());
        }
        return sb.toString();
     }
 }
