package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalUsers;
    private long totalVenues;
    private long totalBookings;
    private double totalRevenue;

    // --- THÊM MỚI CHO BIỂU ĐỒ ---
    private List<ChartDataPoint> userGrowthChart;
    private List<ChartDataPoint> bookingChart;
    private List<ChartDataPoint> revenueChart;
}