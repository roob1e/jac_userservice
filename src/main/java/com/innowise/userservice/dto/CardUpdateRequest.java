package com.innowise.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.YearMonth;

public record CardUpdateRequest(
        @NotBlank
        @Pattern(regexp = "\\d{16}")
        String number,

        @NotBlank
        @Size(max = 255)
        String holder,

        @NotNull
        @FutureOrPresent
        @JsonFormat(pattern = "MM/yy")
        YearMonth expirationDate
) {
}
