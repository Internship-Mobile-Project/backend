package com.badminton.shop.ws_booking_sport.model.venue;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

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

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL)
    private List<AvailabilitySlot> slots;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL)
    private List<PriceRule> priceRules;
}
