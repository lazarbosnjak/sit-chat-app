package ftn.svt.model.dto.chat;

import ftn.svt.model.Message;
import ftn.svt.model.ReceiptStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        ChatMemberInfoResponse sender,
        UUID replyToMessageId,
        UUID forwardedFromMessageId,
        MessageReferenceResponse replyTo,
        MessageReferenceResponse forwardedFrom,
        String content,
        Instant createdAt,
        ReceiptStatus deliveryStatus,
        List<MessageReactionSummaryResponse> reactions
) {
    public static MessageResponse from(Message message) {
        return from(message, ReceiptStatus.SENT);
    }

    public static MessageResponse from(Message message, ReceiptStatus deliveryStatus) {
        return from(message, deliveryStatus, List.of());
    }

    public static MessageResponse from(
            Message message,
            ReceiptStatus deliveryStatus,
            List<MessageReactionSummaryResponse> reactions
    ) {
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
                message.getReplyTo() != null
                        ? MessageReferenceResponse.from(message.getReplyTo())
                        : null,
                message.getForwardedFrom() != null
                        ? MessageReferenceResponse.from(message.getForwardedFrom())
                        : null,
                message.getContent(),
                message.getCreatedAt(),
                deliveryStatus,
                reactions
        );
    }
}
