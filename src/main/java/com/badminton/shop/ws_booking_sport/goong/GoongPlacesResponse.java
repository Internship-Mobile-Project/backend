package com.badminton.shop.ws_booking_sport.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoongPlacesResponse {
    private List<Place> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {
        private String place_id;
        private String name;
        private String formatted_address;
        private Geometry geometry;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Geometry {
            private Location location;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Location {
            private double lat;
            private double lng;
        }
    }
}

