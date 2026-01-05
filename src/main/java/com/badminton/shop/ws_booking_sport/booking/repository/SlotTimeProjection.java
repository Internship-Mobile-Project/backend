package com.badminton.shop.ws_booking_sport.booking.repository;

import java.time.LocalTime;

public interface SlotTimeProjection {
    LocalTime getStartTime();
    LocalTime getEndTime();
}

