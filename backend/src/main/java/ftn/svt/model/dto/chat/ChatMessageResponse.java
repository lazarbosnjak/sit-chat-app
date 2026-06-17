package ftn.svt.model.dto.chat;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(

        UUID id,

        UUID senderId,

        String content,

        UUID replyToMessageId,

        UUID forwardedFromMessageId,

        Instant createdAt
) {
}
