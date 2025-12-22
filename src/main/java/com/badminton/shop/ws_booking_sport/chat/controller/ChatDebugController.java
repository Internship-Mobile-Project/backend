package com.badminton.shop.ws_booking_sport.chat.controller;

import com.badminton.shop.ws_booking_sport.chat.service.MessageSaveQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/chat")
@RequiredArgsConstructor
public class ChatDebugController {

    private final MessageSaveQueue messageSaveQueue;

    @GetMapping("/queue-size")
    public ResponseEntity<Integer> getQueueSize() {
        return ResponseEntity.ok(messageSaveQueue.getQueueSize());
    }

    @PostMapping("/flush")
    public ResponseEntity<String> flushNow() {
        messageSaveQueue.flushNow();
        return ResponseEntity.ok("flush triggered");
    }
}
