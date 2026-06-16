package ftn.svt.model.dto.user;

import ftn.svt.model.User;

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
    public static UserInfoDTO from(User user) {
        return new UserInfoDTO(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getPfpUrl(),
                user.getRole().toString(),
                user.getCreatedAt(),
                user.isEnabled()
        );
    }
}
