package com.badminton.shop.ws_booking_sport.model.booking;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OnlinePayment extends Payment {

    private String method; // MOMO, ZALOPAY, VNPAY
    private String providerResponse;
    private String redirectUrl;
    private LocalDateTime paidAt;

    // Store transaction reference from payment provider (e.g. vnp_TransactionNo)
    private String transactionRef;
}
