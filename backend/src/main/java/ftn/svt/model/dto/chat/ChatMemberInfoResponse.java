package ftn.svt.model.dto.chat;

import ftn.svt.model.ChatRole;

import java.util.UUID;

public record ChatMemberInfoResponse(
        UUID memberId,
        UUID userId,
        String username,
        String fullName,
        String pfpUrl,
        ChatRole role
) {
}
