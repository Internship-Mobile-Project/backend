package com.badminton.shop.ws_booking_sport.handleException;

public class ResourceUnavailableException extends RuntimeException {
    public ResourceUnavailableException(String message) {
        super(message);
    }
}

