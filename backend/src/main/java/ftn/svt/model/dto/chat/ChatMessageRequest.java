package ftn.svt.model.dto.chat;

import java.util.UUID;

public record ChatMessageRequest(
        UUID chatId,
        String content,
        UUID replyToMessageId,
        UUID forwardedFromMessageId
) {}
