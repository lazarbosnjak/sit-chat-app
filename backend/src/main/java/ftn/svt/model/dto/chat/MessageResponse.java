package ftn.svt.model.dto.chat;

import ftn.svt.model.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        ChatMemberInfoResponse sender,
        UUID replyToMessageId,
        UUID forwardedFromMessageId,
        String content,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChat().getId(),
                ChatMemberInfoResponse.from(message.getSender()),
                message.getReplyTo() != null
                        ? message.getReplyTo().getId()
                        : null,
                message.getForwardedFrom() != null
                        ? message.getForwardedFrom().getId()
                        : null,
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
