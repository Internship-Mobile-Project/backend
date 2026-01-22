package com.badminton.shop.ws_booking_sport.goong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Service
public class GoongPlacesService {
    private static final Logger logger = LoggerFactory.getLogger(GoongPlacesService.class);

    @Value("${goong.api.key}")
    private String apiKey;

    @Value("${goong.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GoongPlacesService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String stripTrailingSlashes(String s) {
        if (s == null) return null;
        return s.replaceAll("/+$", "");
    }

    /**
     * Search nearby places by lat/lng and radius in kilometers. If keywords is null or empty, do a single call without keyword.
     * If keywords provided, call nearbysearch once per keyword and merge unique results by place_id.
     */
    public List<GoongPlacesResponse.Place> searchNearby(double lat, double lng, double radiusKm, List<String> keywords) {
        Map<String, GoongPlacesResponse.Place> uniques = new LinkedHashMap<>();

        if (apiUrl == null || apiUrl.isBlank()) {
            logger.error("goong.api.url is not configured");
            return Collections.emptyList();
        }
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("goong.api.key is not configured");
            return Collections.emptyList();
        }

        // validate coordinates and radius
        if (Double.isNaN(lat) || Double.isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            logger.error("Invalid latitude/longitude provided to GoongPlacesService: lat={}, lng={}", lat, lng);
            return Collections.emptyList();
        }
        if (!(radiusKm > 0)) {
            logger.warn("Non-positive radiusKm provided ({}). Using default 5.0 km.", radiusKm);
            radiusKm = 5.0;
        }

        try {
            List<String> keysToUse = (keywords == null || keywords.isEmpty()) ? Collections.singletonList(null) : new ArrayList<>(keywords);
            // cap keywords to avoid excessive external calls
            int maxKeywords = 8;
            if (keysToUse.size() > maxKeywords) keysToUse = keysToUse.subList(0, maxKeywords);

            String base = stripTrailingSlashes(apiUrl);
            String endpoint = base + "/place/nearbysearch";

            for (String kw : keysToUse) {
                // log parameters so troubleshooting shows exactly what was used
                logger.info("Goong call params: lat={}, lng={}, radiusKm={}, keyword={}", lat, lng, radiusKm, kw);

                UriComponentsBuilder b = UriComponentsBuilder.fromUriString(endpoint)
                        .queryParam("location", lat + "," + lng)
                        .queryParam("radius", (int) Math.round(radiusKm * 1000))
                        .queryParam("api_key", apiKey);

                if (kw != null && !kw.isBlank()) {
                    b = b.queryParam("keyword", kw);
                }

                // ensure proper encoding
                URI uri = b.build().encode().toUri();
                logger.info("Calling Goong nearby places: {}", uri);

                try {
                    ResponseEntity<GoongPlacesResponse> resp = restTemplate.getForEntity(uri, GoongPlacesResponse.class);
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && resp.getBody().getResults() != null) {
                        for (GoongPlacesResponse.Place p : resp.getBody().getResults()) {
                            if (p == null) continue;
                            String pid = p.getPlace_id();
                            if (pid == null) {
                                pid = p.getName() + "@" + (p.getGeometry() != null && p.getGeometry().getLocation() != null ? p.getGeometry().getLocation().getLat() + "," + p.getGeometry().getLocation().getLng() : UUID.randomUUID());
                            }
                            uniques.putIfAbsent(pid, p);
                        }
                    }
                } catch (HttpClientErrorException he) {
                    logger.warn("Goong nearbysearch returned {} for URI {} ; responseBody={}", he.getStatusCode(), uri, he.getResponseBodyAsString());
                    // continue with next keyword
                    continue;
                } catch (Exception e) {
                    logger.error("Error calling Goong nearbysearch for URI {}", uri, e);
                    continue;
                }
            }

            return new ArrayList<>(uniques.values());
        } catch (Exception e) {
            logger.error("Error preparing Goong nearby search", e);
            return Collections.emptyList();
        }
    }

    /**
     * Search places for an area (city/district). Use geocode for the area name and then search nearby that centroid.
     * Accepts optional keywords to forward to searchNearby.
     */
    public List<GoongPlacesResponse.Place> searchByArea(String city, String district, List<String> keywords) {
        if ((city == null || city.isBlank()) && (district == null || district.isBlank())) return Collections.emptyList();
        try {
            String q = (district != null && !district.isBlank()) ? district + ", " + city : city;
            String base = stripTrailingSlashes(apiUrl);
            String geocodeEndpoint = base + "/geocode";
            URI uri = UriComponentsBuilder.fromUriString(geocodeEndpoint)
                    .queryParam("address", q)
                    .queryParam("api_key", apiKey)
                    .build()
                    .encode()
                    .toUri();

            logger.info("Calling Goong geocode: {}", uri);
            ResponseEntity<GoongResponse> resp = restTemplate.getForEntity(uri, GoongResponse.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                if ("OK".equals(resp.getBody().getStatus()) && resp.getBody().getResults() != null && !resp.getBody().getResults().isEmpty()) {
                    GoongResponse.GoongLocation loc = resp.getBody().getResults().get(0).getGeometry().getLocation();
                    // then call nearbysearch with a reasonable radius (e.g., 20km)
                    return searchNearby(loc.getLat(), loc.getLng(), 20.0, keywords);
                }
            }
        } catch (HttpClientErrorException he) {
            logger.warn("Goong geocode returned {} for city/district {} / {} ; body={}", he.getStatusCode(), city, district, he.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error calling Goong area search", e);
        }
        return Collections.emptyList();
    }
}
