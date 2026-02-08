package com.finance.tracker.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String message;
    private String token;
    private String role;
    private Long userId;
    private String email;
    private String fullName;
}
