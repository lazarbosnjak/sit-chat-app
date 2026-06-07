package ftn.svt.model.dto.user;

import ftn.svt.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UserInfoDTO(
        String username,

        String firstName,

        String lastName,

        String phoneNumber,

        String email,

        String pfpUrl,

        String role,

        Instant createdAt,

        boolean enabled
) {
}
