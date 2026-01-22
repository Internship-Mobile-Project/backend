package com.badminton.shop.ws_booking_sport.model.core;

import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import com.badminton.shop.ws_booking_sport.model.booking.Review;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.fasterxml.jackson.annotation.JsonIgnore; // <--- IMPORT CÁI NÀY
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Customer extends User {

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Booking> bookings;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Review> reviews;

    @ManyToMany
    @JsonIgnore
    private List<Venue> favouriteVenues;
}