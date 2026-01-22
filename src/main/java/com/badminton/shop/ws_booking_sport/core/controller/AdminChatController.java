package com.badminton.shop.ws_booking_sport.core.controller;

import com.badminton.shop.ws_booking_sport.core.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {
    @Autowired
    private ChatService chatService;

    // API: POST /api/admin/chat/{roomId}/claim
    @PostMapping("/{roomId}/claim")
    public ResponseEntity<?> claimRoom(@PathVariable Long roomId) {
        // Giả sử bạn lấy được ID admin từ SecurityContext (Token đăng nhập)
        // Long currentAdminId = SecurityUtils.getCurrentUserId();
        // Tạm thời mình fix cứng ví dụ là 1
        Integer currentAdminId = 1;

        try {
            chatService.claimRoom(roomId, currentAdminId);
            return ResponseEntity.ok("Nhận việc thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
