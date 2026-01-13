package com.badminton.shop.ws_booking_sport.model.venue;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String type;

    @ManyToOne
    private Venue venue;

    // Single price per hour for the whole field
    private Double pricePerHour;
}
