package com.badminton.shop.ws_booking_sport.handleException;

/**
 * Authentication failure (invalid external token / authentication error) for the application's
 * centralized exception package.
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

