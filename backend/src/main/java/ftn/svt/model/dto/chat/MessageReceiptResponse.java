package ftn.svt.model.dto.chat;

import ftn.svt.model.ChatMember;
import ftn.svt.model.MessageReceipt;
import ftn.svt.model.ReceiptStatus;

import java.time.Instant;
import java.util.UUID;

public record MessageReceiptResponse(
        UUID messageId,
        UUID recipientMemberId,
        String recipientUsername,
        String recipientPfpUrl,
        ReceiptStatus status,
        Instant deliveredAt,
        Instant readAt
) {
    public static MessageReceiptResponse from(MessageReceipt receipt) {
        ChatMember recipient = receipt.getRecipient();

        return new MessageReceiptResponse(
                receipt.getMessage().getId(),
                recipient.getId(),
                recipient.getUser().getUsername(),
                recipient.getUser().getPfpUrl(),
                receipt.getStatus(),
                receipt.getDeliveredAt(),
                receipt.getReadAt()
        );
    }
}
