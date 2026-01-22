package com.badminton.shop.ws_booking_sport.venue.repository;

import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Integer>, JpaSpecificationExecutor<Venue> {
    List<Venue> findByOwnerId(Integer ownerId);
    List<Venue> findBySportContainingIgnoreCase(String sport);

    // Projection interface for native query
    interface VenueDistanceProjection {
        Integer getId();
        Double getDistance();
    }

    /*
     * Native query to compute distance (km) using Haversine formula and return venues within radius.
     * Uses a subselect so we can filter by computed distance.
     * Note: this assumes the Venue table has columns named 'latitude' and 'longitude' (embedded Address fields).
     */
    @Query(value = "SELECT t.id as id, t.distance as distance FROM (SELECT v.id, (6371 * acos(cos(radians(:lat)) * cos(radians(v.latitude)) * cos(radians(v.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(v.latitude)))) AS distance FROM venue v) t WHERE t.distance <= :radiusKm ORDER BY t.distance", nativeQuery = true)
    List<VenueDistanceProjection> findVenueIdsWithinDistance(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusKm") double radiusKm);
}
