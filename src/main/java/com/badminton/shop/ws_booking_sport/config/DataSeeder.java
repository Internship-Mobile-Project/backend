package com.badminton.shop.ws_booking_sport.config;

import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.core.repository.UserRepository;
import com.badminton.shop.ws_booking_sport.enums.AuthProvider;
import com.badminton.shop.ws_booking_sport.enums.Role;
import com.badminton.shop.ws_booking_sport.model.core.Account;
import com.badminton.shop.ws_booking_sport.model.core.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra xem đã có tài khoản admin chưa
        if (accountRepository.findByEmail("admin@system.com").isEmpty()) {

            // 1. Tạo User (Admin Entity)
            Admin adminUser = new Admin();
            adminUser.setName("Super Admin");
            adminUser.setPhone("0999999999");
            adminUser.setActive(true);
            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser.setUpdatedAt(LocalDateTime.now());

            // Lưu User xuống trước để có ID
            userRepository.save(adminUser);

            // 2. Tạo Account liên kết với User đó
            Account adminAccount = new Account();
            adminAccount.setEmail("admin@system.com");
            adminAccount.setPassword(passwordEncoder.encode("admin123")); // Mật khẩu mặc định
            adminAccount.setRole(Role.ADMIN); // Quan trọng nhất
            adminAccount.setVerified(true);
            adminAccount.setAuthProvider(AuthProvider.LOCAL);
            adminAccount.setUser(adminUser); // Link với user ở trên

            accountRepository.save(adminAccount);

            System.out.println("---------------------------------------------");
            System.out.println("ADMIN ACCOUNT CREATED: admin@system.com / admin123");
            System.out.println("---------------------------------------------");
        }
    }
}