package by.assxmblxr.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.YearMonth;

public record CardCreateRequest(
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
