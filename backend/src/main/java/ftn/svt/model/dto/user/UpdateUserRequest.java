package ftn.svt.model.dto.user;

import ftn.svt.model.UserBlockType;
import ftn.svt.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateUserRequest(

        String firstName,

        String lastName,

        @Size(max = 15)
        String phoneNumber,

        @NotBlank
        @Email
        String email,

        @URL
        String pfpUrl,

        @Size(max = 80)
        String status,

        @Size(max = 500)
        String aboutMe,

        boolean enabled,

        UserRole role,

        UserBlockType blockType,

        String blockReason
) {
}
