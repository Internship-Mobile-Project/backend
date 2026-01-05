package com.badminton.shop.ws_booking_sport.model.venue;

import com.badminton.shop.ws_booking_sport.model.core.Address;
import com.badminton.shop.ws_booking_sport.model.core.Owner;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String description;
    private float rating;
    private String sport;
    @Embedded
    private Address address;
    private LocalTime timeOpen;
    private LocalTime timeClose;

    @ElementCollection
    private List<String> imageUrls;

    @ManyToOne
    private Owner owner;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL)
    private List<Field> fields;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
