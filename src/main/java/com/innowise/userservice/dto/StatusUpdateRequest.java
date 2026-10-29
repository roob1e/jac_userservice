package com.innowise.userservice.dto;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull Boolean active
) {
}
