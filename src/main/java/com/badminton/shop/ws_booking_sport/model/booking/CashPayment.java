package com.badminton.shop.ws_booking_sport.model.booking;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CashPayment extends Payment {
    private LocalDateTime paidAt;
}
