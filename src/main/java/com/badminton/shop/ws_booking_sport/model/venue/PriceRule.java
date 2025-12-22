package com.badminton.shop.ws_booking_sport.model.venue;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private double pricePerHour;

    @ManyToOne
    private Field field;
}
