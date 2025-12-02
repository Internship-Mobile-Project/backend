package com.badminton.shop.ws_booking_sport.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class DataResponse implements Serializable {

    private Object metaData;
    private String message;
    private int code;

    @Builder.Default
    private Instant time = Instant.now();

    public static DataResponse success(Object metaData, String message, int code) {
        return DataResponse.builder().metaData(metaData).message(message).code(code).build();
    }

    public static DataResponse error(String message, int code) {
        return DataResponse.builder().message(message).code(code).build();
    }
}
