package com.badminton.shop.ws_booking_sport.chat.service;

import com.badminton.shop.ws_booking_sport.chat.repository.ChatRoomRepository;
import com.badminton.shop.ws_booking_sport.chat.repository.MessageRepository;
import com.badminton.shop.ws_booking_sport.model.chat.ChatRoom;
import com.badminton.shop.ws_booking_sport.model.chat.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class MessageSaveQueue {

    private static final Logger log = LoggerFactory.getLogger(MessageSaveQueue.class);

    private final BlockingQueue<PersistMessage> queue = new LinkedBlockingQueue<>(10_000);
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private volatile boolean running = true;
    private Thread worker;

    public MessageSaveQueue(MessageRepository messageRepository, ChatRoomRepository chatRoomRepository) {
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
    }

    public void enqueue(Message m) {
        if (m == null || m.getChatRoom() == null || m.getChatRoom().getId() == null) {
            log.warn("Attempted to enqueue null/invalid message (chatRoom missing)");
            return;
        }
        PersistMessage pm = new PersistMessage();
        pm.chatRoomId = m.getChatRoom().getId();
        pm.senderId = m.getSenderId();
        pm.senderRole = m.getSenderRole();
        pm.content = m.getContent();
        pm.sentAt = m.getSentAt() == null ? LocalDateTime.now() : m.getSentAt();
        pm.read = m.isRead();

        boolean offered = queue.offer(pm);
        if (!offered) {
            // queue full: log and try to blockingly put (to avoid data loss) but without stalling too long
            log.warn("Message queue is full, blocking put for chatRoomId={} senderId={}. Will wait up to 2 seconds.", pm.chatRoomId, pm.senderId);
            try {
                boolean succeeded = queue.offer(pm, 2, TimeUnit.SECONDS);
                if (!succeeded) {
                    log.error("Failed to enqueue message after waiting: chatRoomId={} senderId={}. Dropping message.", pm.chatRoomId, pm.senderId);
                } else {
                    log.debug("Enqueued message after waiting: chatRoomId={} senderId={}", pm.chatRoomId, pm.senderId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting to enqueue message for chatRoomId={} senderId={}", pm.chatRoomId, pm.senderId);
            }
        } else {
            log.debug("Enqueued message for chatRoomId={} senderId={}", pm.chatRoomId, pm.senderId);
        }
    }

    @PostConstruct
    void start() {
        log.info("Starting MessageSaveQueue worker thread");
        worker = new Thread(() -> {
            while (running || !queue.isEmpty()) {
                try {
                    PersistMessage pm = queue.poll(1, TimeUnit.SECONDS);
                    if (pm == null) continue;
                    try {
                        savePersistMessage(pm);
                    } catch (Exception ex) {
                        log.error("Failed to persist message (will retry): chatRoomId={}, senderId={}, error={}", pm.chatRoomId, pm.senderId, ex.getMessage());
                        // transient failure: re-enqueue with small backoff
                        try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                        // try to re-offer, if it fails then blockingly put it back
                        boolean offered = queue.offer(pm);
                        if (!offered) {
                            try {
                                queue.put(pm);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                log.error("Interrupted while re-queueing message: chatRoomId={}, senderId={}", pm.chatRoomId, pm.senderId);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("MessageSaveQueue worker thread exiting");
        }, "message-save-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void savePersistMessage(PersistMessage pm) {
        // resolve ChatRoom entity
        log.debug("Persisting message for chatRoomId={} senderId={}", pm.chatRoomId, pm.senderId);
        ChatRoom room = chatRoomRepository.findById(pm.chatRoomId).orElse(null);
        if (room == null) {
            // can't save without chat room, drop the message or re-enqueue later
            log.warn("ChatRoom not found when trying to persist message: chatRoomId={}. Dropping message.", pm.chatRoomId);
            return;
        }
        Message msg = new Message();
        msg.setChatRoom(room);
        msg.setSenderId(pm.senderId);
        msg.setSenderRole(pm.senderRole);
        msg.setContent(pm.content);
        msg.setSentAt(pm.sentAt);
        msg.setRead(pm.read);
        messageRepository.save(msg);
        log.debug("Message persisted (chatRoomId={} senderId={})", pm.chatRoomId, pm.senderId);
    }

    /**
     * For debugging: return current queue size (number of messages waiting to be persisted)
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * For debugging: drain the queue and persist synchronously (blocking). Useful to verify persistence immediately.
     */
    public void flushNow() {
        log.info("Flushing {} messages from queue synchronously", queue.size());
        PersistMessage pm;
        while ((pm = queue.poll()) != null) {
            try {
                savePersistMessage(pm);
            } catch (Exception ex) {
                log.error("Failed to persist message while flushing: chatRoomId={}, senderId={}, error={}", pm.chatRoomId, pm.senderId, ex.getMessage());
                // re-enqueue to avoid loss
                try {
                    queue.put(pm);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while re-enqueueing during flush: chatRoomId={}, senderId={}", pm.chatRoomId, pm.senderId);
                }
            }
        }
        log.info("Flush complete. queueSize={}", queue.size());
    }

    @PreDestroy
    void stop() {
        log.info("Stopping MessageSaveQueue worker thread");
        running = false;
        if (worker != null) {
            worker.interrupt();
            try { worker.join(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    // local DTO used in the queue
    static class PersistMessage {
        String chatRoomId;
        Integer senderId;
        String senderRole;
        String content;
        LocalDateTime sentAt;
        boolean read;
    }

}
