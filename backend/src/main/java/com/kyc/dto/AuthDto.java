package com.kyc.dto;

import lombok.Data;

/**
 * DTOs for authentication requests and responses.
 */
public class AuthDto {

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class SignupRequest {
        private String username;
        private String password;
        // Role defaults to MERCHANT if not specified
        private String role = "MERCHANT";
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String username;
        private String role;

        public AuthResponse(String token, String username, String role) {
            this.token = token;
            this.username = username;
            this.role = role;
        }
    }
}
