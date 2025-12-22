package com.badminton.shop.ws_booking_sport.websocket;

import com.badminton.shop.ws_booking_sport.chat.service.ChatService;
import com.badminton.shop.ws_booking_sport.dto.request.MessageRequest;
import com.badminton.shop.ws_booking_sport.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChatController.class);

    private final ChatService chatService;

    // Clients send to /app/chat/{chatRoomId}/send with payload { content }
    @MessageMapping("/chat/{chatRoomId}/send")
    public void handleSend(@DestinationVariable String chatRoomId, MessageRequest req, SimpMessageHeaderAccessor sha) {
        if (req == null || req.getContent() == null) {
            log.debug("Ignored incoming STOMP SEND: empty payload or content is null (chatRoomId={})", chatRoomId);
            return;
        }
        var attrs = sha.getSessionAttributes();
        if (attrs == null) {
            log.warn("Session attributes missing for incoming WS message (chatRoomId={}, content={})", chatRoomId, req.getContent());
            return;
        }
        Object uid = attrs.get("userId");
        Object role = attrs.get("role");
        if (uid == null) {
            log.warn("No userId in WS session attributes - ignoring message (chatRoomId={}, content={})", chatRoomId, req.getContent());
            return;
        }
        int userId;
        try { userId = Integer.parseInt(uid.toString()); } catch (NumberFormatException ex) {
            log.warn("Invalid userId in WS session attributes - ignoring message (chatRoomId={}, userIdRaw={}, content={})", chatRoomId, uid, req.getContent());
            return;
        }
        String roleName = role != null ? role.toString() : null;

        log.debug("Received WS message for chatRoomId={}, from userId={}, role={}, content={}", chatRoomId, userId, roleName, req.getContent());

        // persist and broadcast via ChatService
        chatService.sendMessage(chatRoomId, userId, roleName, req.getContent());
    }
}
