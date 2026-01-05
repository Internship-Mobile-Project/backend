package com.badminton.shop.ws_booking_sport.booking.repository;

import com.badminton.shop.ws_booking_sport.model.venue.Slot;
import com.badminton.shop.ws_booking_sport.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Integer> {

    // Find any slots on the given field/date that overlap with the requested time range
    @Query("SELECT s FROM Slot s WHERE s.field.id = :fieldId AND s.date = :date AND s.startTime < :endTime AND s.endTime > :startTime")
    List<Slot> findOverlappingSlots(@Param("fieldId") Integer fieldId, @Param("date") LocalDate date,
                                    @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    // Projection-based query to fetch only startTime and endTime for booked slots on a field/date
    @Query("SELECT s.startTime AS startTime, s.endTime AS endTime FROM Slot s WHERE s.field.id = :fieldId AND s.date = :date AND s.status = :status ORDER BY s.startTime")
    List<SlotTimeProjection> findTimesByFieldIdAndDateAndStatus(@Param("fieldId") Integer fieldId, @Param("date") LocalDate date, @Param("status") SlotStatus status);

}
