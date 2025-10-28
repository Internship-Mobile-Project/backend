package com.badminton.shop.ws_booking_sport.model;

import lombok.Getter;

@Getter
public enum Role {
    GUEST(1),
    OWNER(2),
    ADMIN(3);

    private final int code;

    Role(int code) {
        this.code = code;
    }

    public static Role fromCode(int code) {
        for (Role r : values()) {
            if (r.code == code) return r;
        }
        throw new IllegalArgumentException("Unknown Role code: " + code);
    }
}

