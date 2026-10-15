package by.assxmblxr.userservice.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record UserWithCardsResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Instant createdAt,
        boolean active,
        List<CardResponse> cards
) implements Serializable {
}
