package ftn.svt.model.dto.chat;

import ftn.svt.model.ChatRole;

import java.util.UUID;

public record ChatMemberInfoResponse(
        UUID userId,
        String username,
        ChatRole role
) {
}
