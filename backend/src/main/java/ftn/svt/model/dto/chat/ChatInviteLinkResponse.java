package ftn.svt.model.dto.chat;

import ftn.svt.model.Chat;

import java.time.Instant;
import java.util.UUID;

public record ChatInviteLinkResponse(
        UUID chatId,
        String token,
        Instant generatedAt
) {
    public static ChatInviteLinkResponse from(Chat chat) {
        return new ChatInviteLinkResponse(
                chat.getId(),
                chat.getInviteToken(),
                chat.getInviteTokenCreatedAt()
        );
    }
}
