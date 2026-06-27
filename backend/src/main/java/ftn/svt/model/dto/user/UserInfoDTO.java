package ftn.svt.model.dto.user;

import ftn.svt.model.User;

import java.time.Instant;
import java.util.UUID;

public record UserInfoDTO(
        UUID id,

        String username,

        String firstName,

        String lastName,

        String phoneNumber,

        String email,

        String pfpUrl,

        String role,

        Instant createdAt,

        boolean enabled,

        String blockType,

        String blockReason,

        Instant blockedAt
) {
    public static UserInfoDTO from(User user) {
        return new UserInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getPfpUrl(),
                user.getRole().toString(),
                user.getCreatedAt(),
                user.isEnabled(),
                user.getBlockType() != null ? user.getBlockType().toString() : null,
                user.getBlockReason(),
                user.getBlockedAt()
        );
    }
}
