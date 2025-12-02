package com.badminton.shop.ws_booking_sport.security;

import com.badminton.shop.ws_booking_sport.enums.Role;
import com.badminton.shop.ws_booking_sport.model.core.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long jwtRefreshExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // public API: generate access and refresh tokens
    public String generateAccessToken(User user, String email, Role role) {
        return generateTokenWithExpiry(user, email, role, jwtExpirationMs);
    }

    public String generateRefreshToken(User user, String email, Role role) {
        return generateTokenWithExpiry(user, email, role, jwtRefreshExpirationMs);
    }

    // backward-compatible alias used by older code
    public String generateToken(User user, String email, Role role) {
        return generateAccessToken(user, email, role);
    }

    // primary token builder
    private String generateTokenWithExpiry(User user, String email, Role role, long expiryMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", user.getName());
        claims.put("email", email);
        claims.put("userId", user.getId());
        if (role != null) claims.put("role", role.name());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // parse all claims (throws runtime exception if invalid)
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractSubject(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        Object email = claims.get("email");
        return email != null ? email.toString() : null;
    }

    public Role extractRole(String token) {
        Claims claims = extractAllClaims(token);
        Object role = claims.get("role");
        if (role == null) return null;
        try {
            return Role.valueOf(role.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // new: expose issued-at timestamp for logout checks
    public Date extractIssuedAt(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getIssuedAt();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date exp = claims.getExpiration();
            if (exp == null || exp.before(new Date())) return false;
            return claims.get("role") != null;
        } catch (Exception ex) {
            return false;
        }
    }
}
