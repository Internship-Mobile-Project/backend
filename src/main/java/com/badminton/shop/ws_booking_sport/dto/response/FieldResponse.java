package com.badminton.shop.ws_booking_sport.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class FieldResponse {
    private Integer id;
    private String name;
    private String type;
    private Integer venueId;
    // include price rules associated with this field
    private List<PriceRuleResponse> priceRules;
}
