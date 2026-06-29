package ftn.svt.model.dto.chat;

import ftn.svt.model.MessageReactionType;

import java.util.UUID;

public record MessageReactionRequest(
        UUID messageId,
        MessageReactionType type
) {}
