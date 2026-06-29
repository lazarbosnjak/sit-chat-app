package ftn.svt.model.dto.chat;

import ftn.svt.model.MessageReactionType;

public record MessageReactionSummaryResponse(
        MessageReactionType type,
        long count,
        boolean reactedByMe
) {}
