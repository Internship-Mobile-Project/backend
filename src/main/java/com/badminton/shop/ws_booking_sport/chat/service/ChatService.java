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
        // try both orientations
        ChatRoom room = chatRoomRepository.findByUserIdAndOwnerIdOrUserIdAndOwnerId(u, f, f, u).orElse(null);
        if (room != null) return room;
        ChatRoom cr = new ChatRoom();
        cr.setUserId(u);
        cr.setOwnerId(f);
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

    // mappers
    public MessageResponse toMessageResponse(Message m) {
        if (m == null) return null;
        MessageResponse mr = new MessageResponse();
        mr.setId(m.getId());
        mr.setChatRoomId(m.getChatRoom() != null ? m.getChatRoom().getId() : null);
        mr.setSenderId(m.getSenderId());
        mr.setSenderRole(m.getSenderRole());
        mr.setContent(m.getContent());
        mr.setSentAt(m.getSentAt());
        mr.setRead(m.isRead());
        return mr;
    }

    public ChatRoomResponse toChatRoomResponse(ChatRoom r) {
        if (r == null) return null;
        ChatRoomResponse cr = new ChatRoomResponse();
        cr.setId(r.getId());
        cr.setUserId(r.getUserId());
        cr.setOwnerId(r.getOwnerId());
        cr.setStatus(r.getStatus());
        cr.setCreatedAt(r.getCreatedAt());
        cr.setUpdatedAt(r.getUpdatedAt());
        // load messages
        List<MessageResponse> msgs = messageRepository.findByChatRoomIdOrderBySentAtAsc(r.getId()).stream().map(this::toMessageResponse).collect(Collectors.toList());
        cr.setMessages(msgs);
        return cr;
    }
}
