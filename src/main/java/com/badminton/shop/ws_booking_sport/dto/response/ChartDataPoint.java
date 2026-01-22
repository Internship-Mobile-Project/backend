package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartDataPoint {
    private LocalDate date;
    private Number value; // Dùng Number để chứa được cả Long (số lượng) và Double (tiền)
    public ChartDataPoint(Date sqlDate, Number value) {
        if (sqlDate != null) {
            // Chuyển đổi từ java.sql.Date/java.util.Date sang LocalDate
            this.date = new java.sql.Date(sqlDate.getTime()).toLocalDate();
        }
        this.value = value;
    }
}