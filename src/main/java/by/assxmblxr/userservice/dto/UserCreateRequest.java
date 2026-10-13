package by.assxmblxr.userservice.dto;

import lombok.Builder;

@Builder
public record UserCreateRequest(
        String firstName,
        String lastName,
        String email
) {
}
