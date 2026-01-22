package com.badminton.shop.ws_booking_sport.admin.service;

import com.badminton.shop.ws_booking_sport.booking.repository.BookingRepository;
import com.badminton.shop.ws_booking_sport.core.repository.UserRepository;
import com.badminton.shop.ws_booking_sport.dto.response.ChartDataPoint;
import com.badminton.shop.ws_booking_sport.dto.response.DashboardStatsResponse;
import com.badminton.shop.ws_booking_sport.handleException.ResourceUnavailableException;
import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import com.badminton.shop.ws_booking_sport.model.core.User;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.badminton.shop.ws_booking_sport.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public DashboardStatsResponse getDashboardStats() {
        long users = userRepository.count();
        long venues = venueRepository.count();
        long bookings = bookingRepository.count();

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0);

        List<Object[]> userRaw = userRepository.getUserGrowthStats(sevenDaysAgo);
        List<Object[]> bookingRaw = bookingRepository.getBookingStats(sevenDaysAgo);
        List<Object[]> revenueRaw = bookingRepository.getRevenueStats(sevenDaysAgo);

        List<ChartDataPoint> userChart = fillMissingDates(mapToChartData(userRaw));
        List<ChartDataPoint> bookingChart = fillMissingDates(mapToChartData(bookingRaw));
        List<ChartDataPoint> revenueChart = fillMissingDates(mapToChartData(revenueRaw));

        double totalRevenue7Days = revenueChart.stream()
                .mapToDouble(point -> point.getValue().doubleValue())
                .sum();

        return DashboardStatsResponse.builder()
                .totalUsers(users)
                .totalVenues(venues)
                .totalBookings(bookings)
                .totalRevenue(totalRevenue7Days)
                .userGrowthChart(userChart)
                .bookingChart(bookingChart)
                .revenueChart(revenueChart)
                .build();
    }

    private List<ChartDataPoint> mapToChartData(List<Object[]> rawData) {
        List<ChartDataPoint> result = new ArrayList<>();
        for (Object[] row : rawData) {
            LocalDate date = null;

            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof java.util.Date) {
                date = new java.sql.Date(((java.util.Date) row[0]).getTime()).toLocalDate();
            } else if (row[0] != null) {
                date = LocalDate.parse(row[0].toString());
            }

            Number value = (row[1] != null) ? (Number) row[1] : 0;

            if (date != null) {
                ChartDataPoint point = new ChartDataPoint();
                point.setDate(date);
                point.setValue(value);
                result.add(point);
            }
        }
        return result;
    }

    private List<ChartDataPoint> fillMissingDates(List<ChartDataPoint> rawData) {
        Map<LocalDate, Number> dataMap = rawData.stream()
                .collect(Collectors.toMap(ChartDataPoint::getDate, ChartDataPoint::getValue));

        List<ChartDataPoint> filledData = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Number value = dataMap.getOrDefault(date, 0);
            filledData.add(new ChartDataPoint(date, value));
        }
        return filledData;
    }
    public void deleteVenue(Integer venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceUnavailableException("Venue not found with id: " + venueId));

        venueRepository.delete(venue);
    }
}