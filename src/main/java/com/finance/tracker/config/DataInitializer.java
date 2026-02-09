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
            if (userRepository.count() == 1) {
                User user = User.builder()
                        .username("admin")
                        .email("kienvt@vt.com")
                        .password(passwordEncoder.encode("123"))
                        .fullName("Admin User")
                        .role(com.finance.tracker.entity.UserRole.USER)
                        .build();

                userRepository.save(user);
                System.out.println("Default user created: username=admin / password=kienvt@123");
            }
        };
    }
}
