package ftn.svt.model.dto.chat;

import ftn.svt.model.Message;

import java.util.UUID;

public record MessageReferenceResponse(
        UUID id,
        String senderFullName,
        String content
) {
    public static MessageReferenceResponse from(Message message) {
        return new MessageReferenceResponse(
                message.getId(),
                message.getSender().getUser().getFullName(),
                message.getContent()
        );
    }
}
