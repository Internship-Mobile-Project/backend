package com.badminton.shop.ws_booking_sport.core.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class VerificationRateLimiter {

    private final ConcurrentMap<String, Attempt> store = new ConcurrentHashMap<>();

    private final int MAX_ATTEMPTS = 3; // max attempts
    private final int WINDOW_MINUTES = 60; // time window in minutes

    public VerificationRateLimiter() {
    }

    /**
     * Returns true if allowed, otherwise false. Also records the attempt when allowed.
     */
    public boolean allow(String email) {
        if (email == null) return false;
        String key = makeKey(email);
        long now = Instant.now().getEpochSecond();

        store.compute(key, (k, cur) -> {
            if (cur == null || cur.expiryEpochSec <= now) {
                // start new window
                return new Attempt(1, now + WINDOW_MINUTES * 60L);
            } else {
                // increment within window
                cur.count++;
                return cur;
            }
        });

        Attempt a = store.get(key);
        return a != null && a.count <= MAX_ATTEMPTS;
    }

    public void reset(String email) {
        if (email == null) return;
        String key = makeKey(email);
        store.remove(key);
    }

    private String makeKey(String email) {
        // simple sanitization to avoid weird characters in keys
        String safe = email.replaceAll("[^a-zA-Z0-9@._-]", "_");
        return "verify:attempts:" + safe;
    }

    private static class Attempt {
        int count;
        long expiryEpochSec;

        Attempt(int count, long expiryEpochSec) {
            this.count = count;
            this.expiryEpochSec = expiryEpochSec;
        }
    }

}
