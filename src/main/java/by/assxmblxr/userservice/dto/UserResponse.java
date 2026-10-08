package by.assxmblxr.userservice.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Instant createdAt
) {
}
