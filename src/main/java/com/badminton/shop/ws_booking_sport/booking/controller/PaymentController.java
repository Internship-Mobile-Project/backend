package com.badminton.shop.ws_booking_sport.booking.controller;

import com.badminton.shop.ws_booking_sport.booking.service.PaymentService;
import com.badminton.shop.ws_booking_sport.config.VNPayConfig;
import com.badminton.shop.ws_booking_sport.dto.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayConfig vnPayConfig;
    private final PaymentService paymentService;

    @GetMapping("/create_payment")
    public ResponseEntity<?> createPayment(
            @RequestParam("amount") long amount,
            @RequestParam("orderId") String orderId,
            HttpServletRequest request
    ) throws UnsupportedEncodingException {

        // 1. Format số tiền theo quy chuẩn VNPay (nhân 100)
        long vnpAmount = amount * 100;

        String vnp_TxnRef = orderId;
        String vnp_IpAddr = getClientIp(request); // Lấy IP chuẩn
        String vnp_TmnCode = vnPayConfig.getTmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (orderId == null || orderId.isEmpty()) {
            // Nếu không có orderId thì random (để test)
            vnp_TxnRef = vnPayConfig.getRandomNumber(8);
        }
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);

        // vnp_OrderInfo không được có dấu hoặc ký tự đặc biệt quá mức
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");

        // Tự động chuyển trang sang chọn ngân hàng (bỏ dòng này nếu muốn user tự chọn bank trên VNPay)
        // vnp_Params.put("vnp_BankCode", "NCB");

        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // 2. Tạo thời gian tạo và hết hạn
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // 3. Sắp xếp tham số (Bắt buộc)
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString())); // Encode dữ liệu

                // Build query string
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        // FIX: Chỉ truyền 1 tham số hashData (key đã nằm trong config)
        String vnp_SecureHash = vnPayConfig.hmacSHA512(hashData.toString());

        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getUrl() + "?" + queryUrl;

        return ResponseEntity.ok(DataResponse.success(paymentUrl, "URL created", HttpStatus.OK.value()));
    }

    @GetMapping("/vnpay_return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        // 1. Lấy tất cả tham số từ VNPay trả về
        Map fields = new HashMap();
        for (Enumeration params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = (String) params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");

        // 2. Xóa 2 tham số hash ra khỏi map để tính lại chữ ký
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        // 3. Tính toán lại chữ ký để so sánh
        String signValue = vnPayConfig.hashAllFields(fields);

        // Hoặc nếu bạn chưa update hashAllFields trong Config, dùng logic thủ công dưới đây (Khuyên dùng đoạn này cho chắc):
        /* List fieldNames = new ArrayList(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                try {
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        String signValue = vnPayConfig.hmacSHA512(sb.toString());
        */

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                // Giao dịch thành công
                String bookingId = request.getParameter("vnp_TxnRef");
                // Chú ý: amount trả về cũng nhân 100, nên chia 100 nếu muốn lưu vào DB chuẩn
                long amount = Long.parseLong(request.getParameter("vnp_Amount")) / 100;
                String txnNo = request.getParameter("vnp_TransactionNo");

                // Cập nhật DB
                paymentService.confirmPayment(bookingId, amount, txnNo);

                return ResponseEntity.ok(DataResponse.success(null, "Payment Success", HttpStatus.OK.value()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DataResponse.error("Payment Failed", HttpStatus.BAD_REQUEST.value()));
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DataResponse.error("Invalid Signature", HttpStatus.BAD_REQUEST.value()));
        }
    }

    // Hàm lấy IP chuẩn
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("0:0:0:0:0:0:0:1")) {
                // Trường hợp chạy localhost
                ipAddress = "127.0.0.1";
            }
        }
        // Trường hợp nhiều IP (qua nhiều proxy), lấy IP đầu tiên
        if (ipAddress != null && ipAddress.length() > 15 && ipAddress.contains(",")) {
            ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
        }
        return ipAddress;
    }
}