package com.badminton.shop.ws_booking_sport.chat.controller;

import com.badminton.shop.ws_booking_sport.chat.service.ChatService;
import com.badminton.shop.ws_booking_sport.dto.request.MessageRequest;
import com.badminton.shop.ws_booking_sport.dto.response.ChatRoomResponse;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import com.badminton.shop.ws_booking_sport.dto.response.MessageResponse;
import com.badminton.shop.ws_booking_sport.enums.Role;
import com.badminton.shop.ws_booking_sport.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final JwtService jwtService;

    // start or get chat with owner
    @PostMapping("/owner/{ownerId}")
    public ResponseEntity<DataResponse> chatWithOwner(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                      @PathVariable String ownerId) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String subject = jwtService.extractSubject(token);
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("Token does not contain user id");
        int userId;
        try { userId = Integer.parseInt(subject); } catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid user id in token subject"); }

        var room = chatService.findOrCreateChatWithOwner(userId, ownerId);
        ChatRoomResponse dto = chatService.toChatRoomResponse(room);
        DataResponse body = DataResponse.success(dto, "Chat room ready", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // start or get chat with friend (another user)
    @PostMapping("/friend/{friendId}")
    public ResponseEntity<DataResponse> chatWithFriend(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                       @PathVariable Integer friendId) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String subject = jwtService.extractSubject(token);
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("Token does not contain user id");
        int userId;
        try { userId = Integer.parseInt(subject); } catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid user id in token subject"); }

        var room = chatService.findOrCreateChatWithFriend(userId, friendId);
        ChatRoomResponse dto = chatService.toChatRoomResponse(room);
        DataResponse body = DataResponse.success(dto, "Chat room ready", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // send message in chat
    @PostMapping("/{chatRoomId}/messages")
    public ResponseEntity<DataResponse> sendMessage(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                    @PathVariable String chatRoomId,
                                                    @RequestBody MessageRequest req) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String subject = jwtService.extractSubject(token);
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("Token does not contain user id");
        int userId;
        try { userId = Integer.parseInt(subject); } catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid user id in token subject"); }

        Role role = jwtService.extractRole(token);
        String roleName = role != null ? role.name() : null;

        var saved = chatService.sendMessage(chatRoomId, userId, roleName, req.getContent());
        MessageResponse dto = chatService.toMessageResponse(saved);
        DataResponse body = DataResponse.success(dto, "Message sent", HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // list messages
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<DataResponse> listMessages(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                     @PathVariable String chatRoomId) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        var msgs = chatService.listMessages(chatRoomId);
        DataResponse body = DataResponse.success(msgs, "Messages fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }

    // get chat room details
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<DataResponse> getChatRoom(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                    @PathVariable String chatRoomId) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        ChatRoomResponse cr = chatService.getChatRoom(chatRoomId);
        DataResponse body = DataResponse.success(cr, "Chat room fetched", HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}
