package com.badminton.shop.ws_booking_sport.booking.service;

import com.badminton.shop.ws_booking_sport.dto.request.ReviewRequest;
import com.badminton.shop.ws_booking_sport.dto.response.ReviewResponse;
import com.badminton.shop.ws_booking_sport.booking.repository.ReviewRepository;
import com.badminton.shop.ws_booking_sport.enums.BookingStatus;
import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import com.badminton.shop.ws_booking_sport.model.booking.Review;
import com.badminton.shop.ws_booking_sport.model.core.Customer;
import com.badminton.shop.ws_booking_sport.booking.repository.BookingRepository;
import com.badminton.shop.ws_booking_sport.core.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public Review createReview(String bookingId, ReviewRequest req, Integer userId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot review a booking that is not completed");
        }
        Customer bookingCustomer = booking.getCustomer();
        if (bookingCustomer == null || !bookingCustomer.getId().equals(userId)) {
            throw new IllegalArgumentException("Only the customer who made the booking can create a review");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setCustomer(bookingCustomer);
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setPhotos(req.getPhotos());
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByBooking(String bookingId) {
        List<Review> list = reviewRepository.findByBookingId(bookingId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewResponse getById(String id) {
        Review r = reviewRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Review not found"));
        return toResponse(r);
    }

    @Transactional
    public Review updateReview(String id, ReviewRequest req, Integer userId) {
        Review r = reviewRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Review not found"));
        if (r.getCustomer() == null || !r.getCustomer().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the customer who created the review can update it");
        }
        r.setRating(req.getRating());
        r.setComment(req.getComment());
        r.setPhotos(req.getPhotos());
        r.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(r);
    }

    @Transactional
    public void deleteReview(String id, Integer userId) {
        Review r = reviewRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Review not found"));
        if (r.getCustomer() == null || !r.getCustomer().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the customer who created the review can delete it");
        }
        reviewRepository.delete(r);
    }

    // expose mapper for reuse
    public ReviewResponse toResponse(Review r) {
        if (r == null) return null;
        ReviewResponse resp = new ReviewResponse();
        resp.setId(r.getId());
        resp.setBookingId(r.getBooking() != null ? r.getBooking().getId() : null);
        resp.setCustomerId(r.getCustomer() != null ? r.getCustomer().getId() : null);
        resp.setCustomerName(r.getCustomer() != null ? r.getCustomer().getName() : null);
        resp.setRating(r.getRating());
        resp.setComment(r.getComment());
        resp.setPhotos(r.getPhotos());
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        return resp;
    }
}
