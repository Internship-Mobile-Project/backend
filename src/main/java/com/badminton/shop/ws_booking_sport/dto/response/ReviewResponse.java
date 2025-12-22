package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private String id;
    private String bookingId;
    private Integer customerId;
    private String customerName;
    private int rating;
    private String comment;
    private List<String> photos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

