package com.innowise.userservice.dto;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record UserResponse(
        Long id,
        String email,
        String name,
        String surname,
        Instant createdAt,
        Instant updatedAt,
        boolean active,
        LocalDate birthDate
) {
}
