package com.badminton.shop.ws_booking_sport.model.booking;

import com.badminton.shop.ws_booking_sport.model.core.Customer;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    private Booking booking;

    @ManyToOne
    private Customer customer;

    private int rating;
    private String comment;

    @ElementCollection
    private List<String> photos;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

