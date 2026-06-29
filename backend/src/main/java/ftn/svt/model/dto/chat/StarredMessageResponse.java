package ftn.svt.model.dto.chat;

import java.time.Instant;
import java.util.UUID;

public record StarredMessageResponse(
        UUID id,
        Instant starredAt,
        ChatInfoResponse chat,
        MessageResponse message
) {}
