package com.badminton.shop.ws_booking_sport.booking.service;

import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import com.badminton.shop.ws_booking_sport.enums.BookingStatus;
import com.badminton.shop.ws_booking_sport.enums.PaymentStatus;
import com.badminton.shop.ws_booking_sport.booking.repository.BookingRepository;
import com.badminton.shop.ws_booking_sport.model.booking.OnlinePayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;

    @Transactional
    public void confirmPayment(String bookingId, long amount, String transactionRef) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getPayment() instanceof OnlinePayment) {
            OnlinePayment onlinePayment = (OnlinePayment) booking.getPayment();

            // Verify amount if necessary (Note: VNPay amount includes 2 decimals, e.g. 1000000 for 10,000)
            // if (amount / 100 != onlinePayment.getAmount()) { ... }

            onlinePayment.setStatus(PaymentStatus.SUCCESS);
            onlinePayment.setPaidAt(LocalDateTime.now());
            onlinePayment.setTransactionRef(transactionRef);
            // Assumption: OnlinePayment has transactionRef field or similar to store provider ref

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        } else {
            throw new IllegalArgumentException("Booking payment method is not ONLINE");
        }
    }
}

