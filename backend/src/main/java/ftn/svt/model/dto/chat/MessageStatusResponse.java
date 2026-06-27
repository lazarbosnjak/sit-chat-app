package ftn.svt.model.dto.chat;

import ftn.svt.model.ReceiptStatus;

import java.util.UUID;

public record MessageStatusResponse(
        UUID messageId,
        ReceiptStatus status
) {}
