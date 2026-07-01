package ftn.svt.model.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank
        String identifier
) {
}
