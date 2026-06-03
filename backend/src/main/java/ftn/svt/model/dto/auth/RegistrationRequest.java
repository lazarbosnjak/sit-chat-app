package ftn.svt.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotBlank
        @Size(min = 8)
        String repeatedPassword,

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
