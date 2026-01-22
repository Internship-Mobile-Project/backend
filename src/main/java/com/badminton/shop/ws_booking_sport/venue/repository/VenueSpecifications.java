package com.badminton.shop.ws_booking_sport.venue.repository;

import com.badminton.shop.ws_booking_sport.model.action.SearchFilter;
import com.badminton.shop.ws_booking_sport.model.venue.Venue;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class VenueSpecifications {

    public static Specification<Venue> bySearchFilter(SearchFilter filter) {
        return (root, query, cb) -> {
            if (query != null) query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filter.getMinRating()));
            }

            if (filter.getSports() != null && !filter.getSports().isEmpty()) {
                List<Predicate> sportPreds = new ArrayList<>();
                for (String s : filter.getSports()) {
                    sportPreds.add(cb.equal(cb.lower(root.get("sport")), s.toLowerCase()));
                }
                predicates.add(cb.or(sportPreds.toArray(new Predicate[0])));
            }

            // time availability: compare venue.timeOpen <= timeStart.time and venue.timeClose >= timeEnd.time
            if (filter.getTimeStart() != null && filter.getTimeEnd() != null) {
                LocalTime tStart = filter.getTimeStart().toLocalTime();
                LocalTime tEnd = filter.getTimeEnd().toLocalTime();
                predicates.add(cb.lessThanOrEqualTo(root.get("timeOpen"), tStart));
                predicates.add(cb.greaterThanOrEqualTo(root.get("timeClose"), tEnd));
            }

            // price range: apply constraints on venue.pricePerHour (moved from fields to venue)
            if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
                if (filter.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerHour"), filter.getMinPrice()));
                }
                if (filter.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("pricePerHour"), filter.getMaxPrice()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Venue> idsIn(List<Integer> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) {
                // return false predicate
                return cb.disjunction();
            }
            return root.get("id").in(ids);
        };
    }
}
