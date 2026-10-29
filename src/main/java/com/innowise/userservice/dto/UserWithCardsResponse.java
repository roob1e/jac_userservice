package com.innowise.userservice.dto;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record UserWithCardsResponse(
        Long id,
        String email,
        String name,
        String surname,
        Instant createdAt,
        Instant updatedAt,
        boolean active,
        LocalDate birthDate,
        List<CardResponse> cards
) implements Serializable {
}
