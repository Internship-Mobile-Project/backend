package com.badminton.shop.ws_booking_sport.model.booking;

import com.badminton.shop.ws_booking_sport.enums.BookingStatus;
import com.badminton.shop.ws_booking_sport.model.core.Customer;
import com.badminton.shop.ws_booking_sport.model.venue.AvailabilitySlot;
import com.badminton.shop.ws_booking_sport.model.venue.Field;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


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

    @ManyToOne
    private AvailabilitySlot slot;

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

