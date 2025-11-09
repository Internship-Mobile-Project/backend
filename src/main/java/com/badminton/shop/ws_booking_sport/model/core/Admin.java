package com.badminton.shop.ws_booking_sport.model.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Admin extends User {
    // Admin có thể có thêm quyền trong tương lai
}
