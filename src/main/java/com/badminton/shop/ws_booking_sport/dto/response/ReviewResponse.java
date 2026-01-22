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
    private String customerAvatar; // added field
    private int rating;
    private String comment;
    private List<String> photos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Use fully qualified name to avoid conflict if Entity name is ambiguous in imports, 
    // but here we know it is likely com.badminton.shop.ws_booking_sport.model.booking.Review
    public static ReviewResponse fromEntity(com.badminton.shop.ws_booking_sport.model.booking.Review entity) {
        ReviewResponse r = new ReviewResponse();
        r.setId(entity.getId());
        if(entity.getBooking() != null) r.setBookingId(entity.getBooking().getId());
        if(entity.getCustomer() != null) {
            r.setCustomerId(entity.getCustomer().getId());
            r.setCustomerName(entity.getCustomer().getName()); // Assuming FullName exists or use Name
            // r.setCustomerAvatar(entity.getCustomer().getAvatarUrl()); // If exists
        }
        r.setRating(entity.getRating());
        r.setComment(entity.getComment());
        r.setPhotos(entity.getPhotos());
        r.setCreatedAt(entity.getCreatedAt());
        r.setUpdatedAt(entity.getUpdatedAt());
        return r;
    }
}
