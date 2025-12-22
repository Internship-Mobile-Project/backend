package com.badminton.shop.ws_booking_sport.chat.repository;

import com.badminton.shop.ws_booking_sport.model.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    Optional<ChatRoom> findByUserIdAndOwnerId(String userId, String ownerId);
    // allow reverse lookup (friend vs user)
    Optional<ChatRoom> findByUserIdAndOwnerIdOrUserIdAndOwnerId(String userId1, String ownerId1, String userId2, String ownerId2);
}

