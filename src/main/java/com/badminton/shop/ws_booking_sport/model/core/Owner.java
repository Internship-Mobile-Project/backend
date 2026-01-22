package com.badminton.shop.ws_booking_sport.model.core;

import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Owner extends User {

    private float rating;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Venue> venues;
}