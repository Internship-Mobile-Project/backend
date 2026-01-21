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

        // 1. Lấy dữ liệu thô (List<Object[]>)
        List<Object[]> userRaw = userRepository.getUserGrowthStats(sevenDaysAgo);
        List<Object[]> bookingRaw = bookingRepository.getBookingStats(sevenDaysAgo);
        List<Object[]> revenueRaw = bookingRepository.getRevenueStats(sevenDaysAgo);

        // 2. Chuyển đổi sang ChartDataPoint và điền khuyết ngày
        List<ChartDataPoint> userChart = fillMissingDates(mapToChartData(userRaw));
        List<ChartDataPoint> bookingChart = fillMissingDates(mapToChartData(bookingRaw));
        List<ChartDataPoint> revenueChart = fillMissingDates(mapToChartData(revenueRaw));

        // 3. Tính tổng doanh thu
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

    // --- HÀM MỚI: Chuyển đổi Object[] sang ChartDataPoint ---
    private List<ChartDataPoint> mapToChartData(List<Object[]> rawData) {
        List<ChartDataPoint> result = new ArrayList<>();
        for (Object[] row : rawData) {
            // row[0] là Date, row[1] là Value (Count hoặc Sum)
            LocalDate date = null;

            // Xử lý an toàn cho Date
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof java.util.Date) {
                date = new java.sql.Date(((java.util.Date) row[0]).getTime()).toLocalDate();
            } else if (row[0] != null) {
                // Trường hợp hiếm: String (tùy database)
                date = LocalDate.parse(row[0].toString());
            }

            // Xử lý an toàn cho Number
            Number value = (row[1] != null) ? (Number) row[1] : 0;

            if (date != null) {
                // Bạn có thể dùng constructor mặc định và set
                ChartDataPoint point = new ChartDataPoint();
                point.setDate(date);
                point.setValue(value);
                result.add(point);
            }
        }
        return result;
    }

    // --- Giữ nguyên hàm fillMissingDates cũ ---
    private List<ChartDataPoint> fillMissingDates(List<ChartDataPoint> rawData) {
        // ... (Code cũ giữ nguyên) ...
        Map<LocalDate, Number> dataMap = rawData.stream()
                .collect(Collectors.toMap(ChartDataPoint::getDate, ChartDataPoint::getValue));

        List<ChartDataPoint> filledData = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Number value = dataMap.getOrDefault(date, 0);
            filledData.add(new ChartDataPoint(date, value)); // Dùng constructor lombok @AllArgsConstructor
        }
        return filledData;
    }
}