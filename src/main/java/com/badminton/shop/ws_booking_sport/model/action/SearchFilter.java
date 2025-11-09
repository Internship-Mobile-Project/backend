package com.badminton.shop.ws_booking_sport.model.action;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class SearchFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String userId;

    @ElementCollection
    private List<String> sports;

    private LocalDateTime date;
    private LocalDateTime timeStart;
    private LocalDateTime timeEnd;

    private Float minPrice;
    private Float maxPrice;
    private Float minRating;

    private String sortBy; // DISTANCE, PRICE, RATING, AVAILABILITY
}

