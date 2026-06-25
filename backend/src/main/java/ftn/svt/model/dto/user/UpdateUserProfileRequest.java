package ftn.svt.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateUserProfileRequest(

        String firstName,

        String lastName,

        @Size(max = 15)
        String phoneNumber,

        @NotBlank
        @Email
        String email,

        @URL
        String pfpUrl
) {
}
