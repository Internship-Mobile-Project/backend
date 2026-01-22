package com.badminton.shop.ws_booking_sport.core.repository;


import com.badminton.shop.ws_booking_sport.enums.RoomStatus;
import com.badminton.shop.ws_booking_sport.model.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByAdminIsNullAndStatus(RoomStatus status);
    List<ChatRoom> findByAdmin_IdAndStatus(Long adminId, RoomStatus status);

}
