package com.badminton.shop.ws_booking_sport.booking.service;

import com.badminton.shop.ws_booking_sport.booking.repository.SlotRepository;
import com.badminton.shop.ws_booking_sport.booking.repository.SlotTimeProjection;
import com.badminton.shop.ws_booking_sport.dto.response.UnavailableSlotResponse;
import com.badminton.shop.ws_booking_sport.enums.SlotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final SlotRepository slotRepository;

    public List<UnavailableSlotResponse> getUnavailableSlots(Integer fieldId, LocalDate date) {
        List<SlotTimeProjection> proj = slotRepository.findTimesByFieldIdAndDateAndStatus(fieldId, date, SlotStatus.BOOKED);
        // Map to simple pairs and sort by startTime
        List<UnavailableSlotResponse> ranges = proj.stream()
                .map(p -> new UnavailableSlotResponse(p.getStartTime(), p.getEndTime()))
                .sorted(Comparator.comparing(UnavailableSlotResponse::getStartTime))
                .toList();

        // Merge overlapping/adjacent ranges
        LinkedList<UnavailableSlotResponse> merged = new LinkedList<>();
        for (UnavailableSlotResponse r : ranges) {
            if (merged.isEmpty()) {
                merged.add(r);
                continue;
            }
            UnavailableSlotResponse last = merged.getLast();
            LocalTime lastEnd = last.getEndTime();
            LocalTime currStart = r.getStartTime();
            LocalTime currEnd = r.getEndTime();
            // If current range overlaps or is adjacent (lastEnd >= currStart) then merge
            if (!lastEnd.isBefore(currStart)) {
                // extend end if needed
                if (currEnd.isAfter(lastEnd)) last.setEndTime(currEnd);
            } else {
                merged.add(r);
            }
        }

        return merged;
    }
}
