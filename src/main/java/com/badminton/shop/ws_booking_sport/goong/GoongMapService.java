package com.badminton.shop.ws_booking_sport.goong;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class GoongMapService {

    private static final Logger logger = LoggerFactory.getLogger(GoongMapService.class);

    @Value("${goong.api.key}")
    private String apiKey;

    @Value("${goong.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GoongMapService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public String removeAccent(String s) {
        if (s == null) return null;
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ','d').replace('Đ','D');
    }
    /**
     * Hàm lấy tọa độ từ địa chỉ
     * @param address: Địa chỉ đầy đủ (vd: "123 Đường A, Quận B, TP.HCM")
     * @return Location object chứa lat, lng hoặc null nếu không tìm thấy
     */
    public GoongResponse.GoongLocation getGeoLocation(String address) {
        try {
            // SỬA QUAN TRỌNG: Tạo URI object để tránh lỗi double encoding tiếng Việt
            String unsignedAddress = removeAccent(address);

            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .path("/geocode")
                    .queryParam("address", unsignedAddress) // Gửi địa chỉ không dấu
                    .queryParam("api_key", apiKey)
                    .build()
                    .toUri();

            logger.info("Calling Goong API: {}", uri);
            logger.debug("Unsigned address sent to Goong: {}", unsignedAddress);

            // Gọi API
            ResponseEntity<GoongResponse> responseEntity = restTemplate.getForEntity(uri, GoongResponse.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                GoongResponse response = responseEntity.getBody();

                logger.debug("Full Goong response: {}", response);

                if ("OK".equals(response.getStatus())
                        && response.getResults() != null
                        && !response.getResults().isEmpty()) {

                    GoongResponse.GoongLocation loc = response.getResults().get(0).getGeometry().getLocation();
                    logger.info(">>> SUCCESS: Coordinates lat={}, lng={}", loc.getLat(), loc.getLng());
                    return loc;
                }
            }

        } catch (Exception e) {
            logger.error("Error geocoding address: {}", address, e);
        }
        return null;
    }
}
