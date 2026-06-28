package com.trading.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDto {
    private UUID id;
    private String email;
    private String phone;
    private String fullName;
    private String kycStatus;
    private String role;
    private boolean active;
    private Instant createdAt;
}
