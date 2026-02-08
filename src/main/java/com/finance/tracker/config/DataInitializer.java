package com.finance.tracker.config;

import com.finance.tracker.entity.User;
import com.finance.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (userRepository.count() == 0) {
                User user = User.builder()
                        .email("kienvt@vt.com")
                        .password(passwordEncoder.encode("kienvt@123"))
                        .fullName("Admin User")
                        .build();

                userRepository.save(user);
                System.out.println("Default user created: admin@example.com / password");
            }
        };
    }
}
