package com.badminton.shop.ws_booking_sport.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class ReviewRequest {
    private int rating;
    private String comment;
    private List<String> photos;
}

