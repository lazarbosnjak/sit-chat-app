package ftn.svt.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegistrationResponse(

        UUID id,

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        String firstName,

        String lastName,

        @Size(max = 15)
        String phoneNumber,

        @NotBlank
        @Email
        String email,

        String pfpUrl

) {
}
