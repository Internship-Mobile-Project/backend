package com.badminton.shop.ws_booking_sport.chat.repository;

import com.badminton.shop.ws_booking_sport.model.chat.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByChatRoomIdOrderBySentAtAsc(String chatRoomId);

    // Fetch the latest message for preview
    Message findTopByChatRoomIdOrderBySentAtDesc(String chatRoomId);
}
