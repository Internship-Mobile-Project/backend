package com.badminton.shop.ws_booking_sport.model.action;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AreaSearchFilter extends SearchFilter {
    private String city;
    private String district;
}

