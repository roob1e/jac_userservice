package com.innowise.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.Instant;
import java.time.YearMonth;

public record CardResponse(
        Long id,
        Long userId,
        String number,
        String holder,
        @JsonFormat(pattern = "MM/yy") YearMonth expirationDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
