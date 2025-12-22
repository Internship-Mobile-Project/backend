package com.badminton.shop.ws_booking_sport.websocket;

import com.badminton.shop.ws_booking_sport.security.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String token = null;
        // Try Authorization header
        if (request.getHeaders().containsKey("Authorization")) {
            String auth = request.getHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) token = auth.substring(7);
        }
        // Fallback: token query param
        if (token == null && request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletReq = ((ServletServerHttpRequest) request).getServletRequest();
            String qp = servletReq.getParameter("token");
            if (qp != null && !qp.isBlank()) token = qp;
        }

        if (token == null || !jwtService.isTokenValid(token)) {
            return false; // reject handshake
        }

        String subject = jwtService.extractSubject(token);
        String role = jwtService.extractRole(token) != null ? jwtService.extractRole(token).name() : null;
        attributes.put("userId", subject);
        attributes.put("role", role);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
