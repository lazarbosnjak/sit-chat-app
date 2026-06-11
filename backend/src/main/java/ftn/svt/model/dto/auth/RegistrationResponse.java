package ftn.svt.model.dto.auth;

import ftn.svt.model.RegistrationRequestForm;
import ftn.svt.model.RegistrationRequestFormStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegistrationResponse(

        UUID requestId,

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

        String pfpUrl,

        RegistrationRequestFormStatus status

) {
    public static RegistrationResponse from(RegistrationRequestForm form) {
        return new RegistrationResponse(
                form.getRequestId(),
                form.getUsername(),
                form.getFirstName(),
                form.getLastName(),
                form.getPhoneNumber(),
                form.getEmail(),
                form.getPfpUrl(),
                form.getStatus()
        );
    }
}
