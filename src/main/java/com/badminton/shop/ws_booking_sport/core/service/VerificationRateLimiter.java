package com.badminton.shop.ws_booking_sport.core.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class VerificationRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private final int MAX_ATTEMPTS = 3; // max attempts
    private final int WINDOW_MINUTES = 60; // time window in minutes

    public VerificationRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true if allowed, otherwise false. Also records the attempt when allowed.
     */
    public boolean allow(String email) {
        if (email == null) return false;
        String key = makeKey(email);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) return false; // Redis issue
        if (count == 1L) {
            // set expiry for the key on first increment
            redisTemplate.expire(key, WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        return count <= MAX_ATTEMPTS;
    }

    public void reset(String email) {
        if (email == null) return;
        String key = makeKey(email);
        redisTemplate.delete(key);
    }

    private String makeKey(String email) {
        // simple sanitization to avoid weird characters in keys
        String safe = email.replaceAll("[^a-zA-Z0-9@._-]", "_");
        return "verify:attempts:" + safe;
    }

}

