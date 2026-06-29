package ftn.svt.model.dto.chat;

import java.util.UUID;

public record MessageStarUpdateResponse(
        UUID messageId,
        boolean starred
) {}
