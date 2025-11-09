// ...existing code...
package com.badminton.shop.ws_booking_sport.security;

import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtService.isTokenValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String email = jwtService.extractEmail(token);
                    Role role = jwtService.extractRole(token);
                    if (email != null && role != null) {
                        var accountOpt = accountRepository.findByEmail(email);
                        if (accountOpt.isPresent()) {
                            var authority = new SimpleGrantedAuthority("ROLE_" + role.name());
                            var auth = new UsernamePasswordAuthenticationToken(email, null, Collections.singletonList(authority));
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                }
            } catch (Exception ex) {
                // if token invalid or other error, do not set authentication
            }
        }

        filterChain.doFilter(request, response);
    }
}

