package com.badminton.shop.ws_booking_sport.core.service;

import com.badminton.shop.ws_booking_sport.core.repository.AdminRepository;
import com.badminton.shop.ws_booking_sport.core.repository.ChatRoomRepository;
import com.badminton.shop.ws_booking_sport.enums.RoomStatus;
import com.badminton.shop.ws_booking_sport.model.chat.ChatRoom;
import com.badminton.shop.ws_booking_sport.model.core.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService {
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // Hàm nhận việc (Claim)
    public ChatRoom claimRoom(Long roomId, Integer adminId) {
        // 1. Tìm phòng chat
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng chat"));

        // 2. CHECK QUAN TRỌNG: Đã có ai nhận chưa?
        if (room.getAdmin() != null) {
            throw new RuntimeException("Phòng này đã có người nhận rồi!");
        }

        // 3. Tìm thông tin Admin đang đăng nhập
        Admin currentAdmin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin không tồn tại"));

        // 4. Gán Admin vào phòng & Chuyển trạng thái
        room.setAdmin(currentAdmin);
        room.setStatus(RoomStatus.ACTIVE);

        ChatRoom savedRoom = chatRoomRepository.save(room);

        // 5. Bắn Socket báo cho các Admin khác biết để xóa khỏi danh sách chờ
        // Payload chỉ cần gửi ID phòng là đủ
        Map<String, Object> socketPayload = new HashMap<>();
        socketPayload.put("type", "ROOM_CLAIMED");
        socketPayload.put("roomId", roomId);

        // Gửi tới kênh chung của Admin
        messagingTemplate.convertAndSend("/topic/admin/global-updates", socketPayload);

        return savedRoom;
    }
}
