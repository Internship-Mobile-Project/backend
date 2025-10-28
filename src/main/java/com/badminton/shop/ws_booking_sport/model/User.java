package com.badminton.shop.ws_booking_sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    /** store role as integer in DB: 1=GUEST,2=OWNER,3=ADMIN */
    @Column(nullable = false)
    private int role;

    public User() {}

    public User(String email, String password, int role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    @Transient
    public Role getRoleEnum() {
        return Role.fromCode(this.role);
    }
}

