package com.badminton.shop.ws_booking_sport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Bước 1: Đăng ký điểm kết nối (Connection Endpoint)
     * Client (Flutter) sẽ kết nối vào đường dẫn này đầu tiên.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đường dẫn kết nối: ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Quan trọng: Cho phép Flutter từ mọi nguồn kết nối (tránh lỗi CORS)
                .withSockJS(); // Hỗ trợ fallback cho trình duyệt cũ (nếu có web admin)

        // Dành riêng cho Flutter (Native connection thường không cần SockJS)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Bước 2: Cấu hình Broker (Người đưa thư)
     * Quy định các tiền tố (prefix) cho đường đi của tin nhắn.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. Prefix dành cho Server GỬI VỀ Client (Subscribe)
        // /topic: Thường dùng cho thông báo chung (1 người nói, nhiều người nghe - như cái chức năng Nhận việc của bạn)
        // /queue: Thường dùng cho tin nhắn riêng tư (1-1)
        registry.enableSimpleBroker("/topic", "/queue");

        // 2. Prefix dành cho Client GỬI LÊN Server (Publish)
        // Ví dụ: Client gửi tin nhắn thì gửi vào /app/chat/...
        registry.setApplicationDestinationPrefixes("/app");

        // 3. (Tùy chọn) Prefix dành cho tin nhắn riêng tư cụ thể user
        registry.setUserDestinationPrefix("/user");
    }
}