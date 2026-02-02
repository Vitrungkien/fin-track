package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.RegisterRequest;
import com.finance.tracker.dto.response.AuthResponse;
import com.finance.tracker.entity.User;
import com.finance.tracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
                .message("User registered successfully")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build());
    }
}
