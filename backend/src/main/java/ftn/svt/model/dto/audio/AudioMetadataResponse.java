package ftn.svt.model.dto.audio;

import java.time.Instant;
import java.util.UUID;

public record AudioMetadataResponse(
        UUID id,
        UUID messageId,
        UUID chatId,
        UUID senderId,
        String contentType,
        long sizeBytes,
        int durationMs,
        Instant createdAt
) {
}
