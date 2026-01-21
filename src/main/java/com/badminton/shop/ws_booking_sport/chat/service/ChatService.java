package com.badminton.shop.ws_booking_sport.chat.service;

import com.badminton.shop.ws_booking_sport.chat.repository.ChatRoomRepository;
import com.badminton.shop.ws_booking_sport.chat.repository.MessageRepository;
import com.badminton.shop.ws_booking_sport.dto.response.ChatRoomResponse;
import com.badminton.shop.ws_booking_sport.dto.response.MessageResponse;
import com.badminton.shop.ws_booking_sport.model.chat.ChatRoom;
import com.badminton.shop.ws_booking_sport.model.chat.Message;
import com.badminton.shop.ws_booking_sport.enums.ChatStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate simpMessagingTemplate; // used to broadcast
    private final MessageSaveQueue messageSaveQueue; // background persistence queue

    @Transactional
    public ChatRoom findOrCreateChatWithOwner(Integer userId, String ownerId) {
        String userIdStr = String.valueOf(userId);
        ChatRoom room = chatRoomRepository.findByUserIdAndOwnerId(userIdStr, ownerId).orElse(null);
        if (room != null) return room;
        ChatRoom cr = new ChatRoom();
        cr.setUserId(userIdStr);
        cr.setOwnerId(ownerId);
        cr.setStatus(ChatStatus.ACTIVE);
        cr.setCreatedAt(LocalDateTime.now());
        cr.setUpdatedAt(LocalDateTime.now());
        return chatRoomRepository.save(cr);
    }

    @Transactional
    public ChatRoom findOrCreateChatWithFriend(Integer userId, Integer friendId) {
        String u = String.valueOf(userId);
        String f = String.valueOf(friendId);

        // Check finding existing chat using friendId logic
        ChatRoom room = chatRoomRepository.findExistingFriendChat(u, f).orElse(null);
        if (room != null) return room;

        ChatRoom cr = new ChatRoom();
        cr.setUserId(u);
        cr.setFriendId(f); // Use friendId field
        cr.setOwnerId(null); // Explicitly null
        cr.setStatus(ChatStatus.ACTIVE);
        cr.setCreatedAt(LocalDateTime.now());
        cr.setUpdatedAt(LocalDateTime.now());
        return chatRoomRepository.save(cr);
    }

    @Transactional
    public Message sendMessage(String chatRoomId, Integer senderId, String senderRole, String content) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new IllegalArgumentException("Chat room not found"));
        // validate sender belongs to chat
        String sid = String.valueOf(senderId);
        if (!sid.equals(room.getUserId()) && !sid.equals(room.getOwnerId())) {
            throw new IllegalArgumentException("Sender is not part of this chat");
        }
        Message m = new Message();
        m.setChatRoom(room);
        m.setSenderId(senderId);
        m.setSenderRole(senderRole);
        m.setContent(content);
        m.setSentAt(LocalDateTime.now());
        m.setRead(false);

        // immediate delivery to subscribers (fast UX) - do NOT wait for DB save
        try {
            MessageResponse mr = toMessageResponse(m);
            String destDot = "/topic/chat." + chatRoomId;
            String destSlash = "/topic/chat/" + chatRoomId;
            simpMessagingTemplate.convertAndSend(destDot, mr);
            simpMessagingTemplate.convertAndSend(destSlash, mr);
            log.debug("Broadcasted message to destinations: {} and {} (chatRoomId={}, sender={})", destDot, destSlash, chatRoomId, senderId);
        } catch (Exception ex) {
            log.error("Failed to broadcast message for chatRoomId={} sender={}, error={}", chatRoomId, senderId, ex.getMessage());
        }

        // enqueue for background persistence
        messageSaveQueue.enqueue(m);
        log.debug("Enqueued message for background persistence (chatRoomId={}, sender={})", chatRoomId, senderId);

        // update room's updatedAt and persist it (keeps chat metadata consistent)
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        // return the unsaved message (id will be null until the queue saves it)
        return m;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(String chatRoomId) {
        List<Message> msgs = messageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId);
        return msgs.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoom(String chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new IllegalArgumentException("Chat room not found"));
        return toChatRoomResponse(room);
    }

    public ChatRoomResponse toChatRoomResponse(ChatRoom room) {
        return toChatRoomResponse(room, false);
    }

    public ChatRoomResponse toChatRoomResponse(ChatRoom room, boolean listOnlyLastMessage) {
        ChatRoomResponse dto = new ChatRoomResponse();
        dto.setId(room.getId());
        dto.setUserId(room.getUserId());
        dto.setOwnerId(room.getOwnerId());
        dto.setFriendId(room.getFriendId());
        dto.setStatus(room.getStatus());
        dto.setCreatedAt(room.getCreatedAt());
        dto.setUpdatedAt(room.getUpdatedAt());

        if (listOnlyLastMessage) {
            Message lastMsg = messageRepository.findTopByChatRoomIdOrderBySentAtDesc(room.getId());
            if (lastMsg != null) {
                dto.setMessages(List.of(toMessageResponse(lastMsg)));
            } else {
                dto.setMessages(List.of());
            }
        } else {
            // Fetch all messages (for chat details)
            // Note: room.getMessages() might be lazy, but here we likely want all or we use repository
            List<Message> msgs = messageRepository.findByChatRoomIdOrderBySentAtAsc(room.getId());
            if (msgs != null) {
                dto.setMessages(msgs.stream().map(this::toMessageResponse).collect(Collectors.toList()));
            } else {
                dto.setMessages(List.of());
            }
        }
        return dto;
    }

    public MessageResponse toMessageResponse(Message m) {
        String chatRoomId = (m.getChatRoom() != null) ? m.getChatRoom().getId() : null;
        return new MessageResponse(m.getId(), chatRoomId, m.getSenderId(), m.getSenderRole(), m.getContent(), m.getSentAt(), m.isRead());
    }

    public List<ChatRoomResponse> getChatRoomsForUser(Integer userId) {
        String uid = String.valueOf(userId);
        List<ChatRoom> rooms = chatRoomRepository.findByUserParticipation(uid);
        return rooms.stream()
                .map(room -> toChatRoomResponse(room, true)) // true = fetch only last message
                .collect(Collectors.toList());
    }
}
