package com.badminton.shop.ws_booking_sport.admin.service;

import com.badminton.shop.ws_booking_sport.booking.repository.BookingRepository;
import com.badminton.shop.ws_booking_sport.core.repository.UserRepository;
import com.badminton.shop.ws_booking_sport.handleException.ResourceUnavailableException;
import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import com.badminton.shop.ws_booking_sport.model.core.User;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.badminton.shop.ws_booking_sport.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User toggleUserStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceUnavailableException("User not found with id: " + userId));

        user.setActive(!user.isActive());

        return userRepository.save(user);
    }
    public Page<Venue> getAllVenues(Pageable pageable) {
        return venueRepository.findAll(pageable);
    }

    public Page<Booking> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable);
    }
}