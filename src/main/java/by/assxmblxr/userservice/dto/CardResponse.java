package by.assxmblxr.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.YearMonth;

public record CardResponse(
        Long id,
        Long userId,
        String number,
        String holder,
        @JsonFormat(pattern = "MM/yy") YearMonth expirationDate,
        boolean active,
        Instant createdAt
) {
}
