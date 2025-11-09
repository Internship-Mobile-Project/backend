package com.badminton.shop.ws_booking_sport.model.action;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LocationSearchFilter extends SearchFilter {
    private Double latitude;
    private Double longitude;
    private Float maxDistanceKm;
}

