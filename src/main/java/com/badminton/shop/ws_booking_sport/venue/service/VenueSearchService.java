package com.badminton.shop.ws_booking_sport.venue.service;

import com.badminton.shop.ws_booking_sport.dto.response.VenuesResponse;
import com.badminton.shop.ws_booking_sport.goong.GoongMapService;
import com.badminton.shop.ws_booking_sport.goong.GoongResponse;
import com.badminton.shop.ws_booking_sport.model.action.AreaSearchFilter;
import com.badminton.shop.ws_booking_sport.model.action.LocationSearchFilter;
import com.badminton.shop.ws_booking_sport.model.action.SearchFilter;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import com.badminton.shop.ws_booking_sport.venue.repository.VenueRepository;
import com.badminton.shop.ws_booking_sport.venue.repository.VenueSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class VenueSearchService {

    private final VenueRepository venueRepository;
    private final GoongMapService goongMapService;

    public List<VenuesResponse> search(SearchFilter filter) {
        if (filter == null) throw new IllegalArgumentException("SearchFilter is required");

        // Resolve center coordinates
        Double centerLat = null, centerLng = null;
        double radiusKm = 5.0; // default

        if (filter instanceof LocationSearchFilter) {
            LocationSearchFilter lf = (LocationSearchFilter) filter;
            if (lf.getLatitude() == null || lf.getLongitude() == null) {
                throw new IllegalArgumentException("Latitude and longitude required for LocationSearchFilter");
            }
            centerLat = lf.getLatitude();
            centerLng = lf.getLongitude();
            if (lf.getMaxDistanceKm() != null && lf.getMaxDistanceKm() > 0) radiusKm = lf.getMaxDistanceKm().doubleValue();
        } else if (filter instanceof AreaSearchFilter) {
            AreaSearchFilter af = (AreaSearchFilter) filter;
            String q = (af.getDistrict() != null && !af.getDistrict().isBlank()) ? af.getDistrict() + ", " + af.getCity() : af.getCity();
            GoongResponse.Location loc = null;
            try {
                loc = goongMapService.getGeoLocation(q);
            } catch (Exception e) {
                // fall through
            }
            if (loc == null) {
                throw new IllegalArgumentException("Unable to geocode area: " + q);
            }
            centerLat = loc.getLat();
            centerLng = loc.getLng();
            radiusKm = 20.0; // area default radius
        } else {
            throw new IllegalArgumentException("Unsupported SearchFilter type");
        }

        // Step 2: DB native query to get venue ids within distance and their distances
        List<VenueRepository.VenueDistanceProjection> projections = venueRepository.findVenueIdsWithinDistance(centerLat, centerLng, radiusKm);
        if (projections == null || projections.isEmpty()) return Collections.emptyList();

        // Map id -> distance
        Map<Integer, Double> idToDistance = new HashMap<>();
        List<Integer> ids = new ArrayList<>();
        for (VenueRepository.VenueDistanceProjection p : projections) {
            if (p.getId() != null) {
                ids.add(p.getId());
                idToDistance.put(p.getId(), p.getDistance());
            }
        }
        if (ids.isEmpty()) return Collections.emptyList();

        // Build Specification: attribute filters + idsIn
        Specification<Venue> baseSpec = VenueSpecifications.bySearchFilter(filter);
        Specification<Venue> spec = (baseSpec != null) ? baseSpec.and(VenueSpecifications.idsIn(ids)) : VenueSpecifications.idsIn(ids);

        List<Venue> venues = venueRepository.findAll(spec);
        if (venues.isEmpty()) return Collections.emptyList();

        // Map to response and set pricePerHour from venue
        List<VenuesResponse> resp = new ArrayList<>();
        for (Venue v : venues) {
            VenuesResponse vr = new VenuesResponse();
            vr.setId(v.getId());
            vr.setName(v.getName());
            vr.setSport(v.getSport());
            vr.setRating(v.getRating());
            vr.setAddress(v.getAddress());
            vr.setLatitude(v.getAddress() != null ? v.getAddress().getLatitude() : null);
            vr.setLongitude(v.getAddress() != null ? v.getAddress().getLongitude() : null);
            vr.setDistanceKm(idToDistance.get(v.getId()));

            // price now stored at venue level
            Double price = v.getPricePerHour();
            vr.setMinPricePerHour(price);
            vr.setPricePerHour(price != null ? price : 0.0);
            resp.add(vr);
        }

        // Step 3: Sorting
        String sortBy = filter.getSortBy();
        if (sortBy != null) {
            switch (sortBy.toUpperCase()) {
                case "DISTANCE":
                    resp.sort(Comparator.comparing(r -> r.getDistanceKm() == null ? Double.MAX_VALUE : r.getDistanceKm()));
                    break;
                case "PRICE":
                    resp.sort(Comparator.comparing(r -> r.getMinPricePerHour() == null ? Double.MAX_VALUE : r.getMinPricePerHour()));
                    break;
                case "RATING":
                    resp.sort(Comparator.comparing(VenuesResponse::getRating).reversed());
                    break;
                default:
                    break;
            }
        }

        return resp;
    }

    public Map<String, Object> searchDebug(SearchFilter filter) {
        Map<String, Object> out = new HashMap<>();
        if (filter == null) throw new IllegalArgumentException("SearchFilter is required");

        // Resolve center as in search
        Double centerLat = null, centerLng = null;
        double radiusKm = 5.0;
        if (filter instanceof LocationSearchFilter) {
            LocationSearchFilter lf = (LocationSearchFilter) filter;
            centerLat = lf.getLatitude();
            centerLng = lf.getLongitude();
            if (lf.getMaxDistanceKm() != null && lf.getMaxDistanceKm() > 0) radiusKm = lf.getMaxDistanceKm();
        } else if (filter instanceof AreaSearchFilter) {
            AreaSearchFilter af = (AreaSearchFilter) filter;
            String q = (af.getDistrict() != null && !af.getDistrict().isBlank()) ? af.getDistrict() + ", " + af.getCity() : af.getCity();
            GoongResponse.Location loc = goongMapService.getGeoLocation(q);
            if (loc != null) {
                centerLat = loc.getLat(); centerLng = loc.getLng(); radiusKm = 20.0;
            }
            out.put("geocodeQuery", q);
            out.put("geocodeLocation", loc);
        }

        out.put("centerLat", centerLat);
        out.put("centerLng", centerLng);
        out.put("radiusKm", radiusKm);

        if (centerLat == null || centerLng == null) {
            out.put("error", "no center resolved");
            return out;
        }

        List<VenueRepository.VenueDistanceProjection> projections = venueRepository.findVenueIdsWithinDistance(centerLat, centerLng, radiusKm);
        out.put("projectionsCount", projections == null ? 0 : projections.size());
        List<Map<String, Object>> ps = new ArrayList<>();
        if (projections != null) {
            for (var p : projections) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getId()); m.put("distance", p.getDistance());
                ps.add(m);
            }
        }
        out.put("projections", ps);

        // Now apply DB filters
        List<Integer> ids = projections.stream().map(VenueRepository.VenueDistanceProjection::getId).toList();
        Specification<Venue> baseSpec = VenueSpecifications.bySearchFilter(filter);
        Specification<Venue> spec = (baseSpec != null) ? baseSpec.and(VenueSpecifications.idsIn(ids)) : VenueSpecifications.idsIn(ids);
        List<Venue> dbFiltered = venueRepository.findAll(spec);
        out.put("dbFilteredCount", dbFiltered == null ? 0 : dbFiltered.size());

        // Map a small merged result
        List<Map<String, Object>> merged = new ArrayList<>();
        if (dbFiltered != null) {
            for (Venue v : dbFiltered) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", v.getId()); m.put("name", v.getName());
                m.put("distance", idToDistanceSafe(projections, v.getId()));
                merged.add(m);
            }
        }
        out.put("merged", merged);
        return out;
    }

    private Double idToDistanceSafe(List<VenueRepository.VenueDistanceProjection> projections, Integer id) {
        if (projections == null) return null;
        for (var p : projections) if (Objects.equals(p.getId(), id)) return p.getDistance();
        return null;
    }
}
