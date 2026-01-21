package com.badminton.shop.ws_booking_sport.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MomoCreateResponse {
    @JsonProperty("partnerCode")
    private String partnerCode;
    @JsonProperty("accessKey")
    private String accessKey;
    @JsonProperty("requestId")
    private String requestId;
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("payUrl")
    private String payUrl;
    @JsonProperty("errorCode")
    private Integer errorCode;
    @JsonProperty("message")
    private String message;
    @JsonProperty("localMessage")
    private String localMessage;
}

