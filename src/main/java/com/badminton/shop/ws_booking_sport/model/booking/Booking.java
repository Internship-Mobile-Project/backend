package com.badminton.shop.ws_booking_sport.model.booking;

import com.badminton.shop.ws_booking_sport.enums.BookingStatus;
import com.badminton.shop.ws_booking_sport.model.core.Customer;
import com.badminton.shop.ws_booking_sport.model.venue.Slot;
import com.badminton.shop.ws_booking_sport.model.venue.Field;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private Field field;

    @OneToMany
    private List<Slot> slot;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private double totalPrice;
    private String cancelReason;
    private LocalDateTime bookedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

