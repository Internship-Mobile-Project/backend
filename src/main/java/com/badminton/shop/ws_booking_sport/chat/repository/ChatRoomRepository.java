package com.badminton.shop.ws_booking_sport.chat.repository;

import com.badminton.shop.ws_booking_sport.model.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    Optional<ChatRoom> findByUserIdAndOwnerId(String userId, String ownerId);

    // Friend chat bidirectional search
    @Query("SELECT c FROM ChatRoom c WHERE (c.userId = :userId AND c.friendId = :friendId) OR (c.userId = :friendId AND c.friendId = :userId)")
    Optional<ChatRoom> findExistingFriendChat(@Param("userId") String userId, @Param("friendId") String friendId);

    // Find all chat rooms for a user (as userId, ownerId, or friendId)
    @Query("SELECT c FROM ChatRoom c WHERE c.userId = :id OR c.ownerId = :id OR c.friendId = :id ORDER BY c.updatedAt DESC")
    List<ChatRoom> findByUserParticipation(@Param("id") String id);

    // allow reverse lookup (friend vs user) - Keeping for backward compatibility or safely removing if usages are gone
    // Optional<ChatRoom> findByUserIdAndOwnerIdOrUserIdAndOwnerId(String userId1, String ownerId1, String userId2, String ownerId2);
}
