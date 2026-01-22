package com.badminton.shop.ws_booking_sport.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Bỏ qua các trường không cần thiết
public class GoongResponse {
    private List<Result> results;
    private String status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private Geometry geometry;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        private GoongLocation location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoongLocation {
        // Use double for higher precision
        private double lat;
        private double lng;
    }
}