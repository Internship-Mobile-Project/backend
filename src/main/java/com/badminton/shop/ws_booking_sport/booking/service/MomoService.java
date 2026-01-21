package com.badminton.shop.ws_booking_sport.booking.service;

import com.badminton.shop.ws_booking_sport.dto.response.MomoCreateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class MomoService {

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.return-url}")
    private String defaultReturnUrl;

    @Value("${momo.notify-url}")
    private String notifyUrl;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public MomoCreateResponse createOrder(String amount, String orderInfo, String orderId, String returnUrl) throws Exception {
        String requestId = UUID.randomUUID().toString();
        if (orderId == null || orderId.isBlank()) orderId = "ORDER-" + System.currentTimeMillis();
        String redirectUrl = (returnUrl == null || returnUrl.isBlank()) ? defaultReturnUrl : returnUrl;
        String extraData = ""; // optional

        String requestType = "captureWallet";

        // Signature format:
        // partnerCode=$partnerCode&accessKey=$accessKey&requestId=$requestId&amount=$amount&orderId=$orderId&orderInfo=$orderInfo&returnUrl=$returnUrl&notifyUrl=$notifyUrl&extraData=$extraData
        String rawSignature = "partnerCode=" + partnerCode +
                "&accessKey=" + accessKey +
                "&requestId=" + requestId +
                "&amount=" + amount +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&returnUrl=" + redirectUrl +
                "&notifyUrl=" + notifyUrl +
                "&extraData=" + extraData;

        String signature = hmacSHA256(rawSignature, secretKey);

        // build payload
        var payload = new java.util.HashMap<String, Object>();
        payload.put("partnerCode", partnerCode);
        payload.put("accessKey", accessKey);
        payload.put("requestId", requestId);
        payload.put("amount", amount);
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("returnUrl", redirectUrl);
        payload.put("notifyUrl", notifyUrl);
        payload.put("extraData", extraData);
        payload.put("signature", signature);
        payload.put("requestType", requestType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(payload), headers);

        ResponseEntity<String> resp = rest.postForEntity(endpoint, entity, String.class);

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            return mapper.readValue(resp.getBody(), MomoCreateResponse.class);
        } else {
            MomoCreateResponse error = new MomoCreateResponse();
            error.setErrorCode(-1);
            error.setMessage("Momo API returned non-200");
            return error;
        }
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        // Momo expects hex lowercase
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

