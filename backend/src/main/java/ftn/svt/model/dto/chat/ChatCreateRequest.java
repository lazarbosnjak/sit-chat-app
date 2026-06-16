package ftn.svt.model.dto.chat;

import ftn.svt.model.ChatType;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record ChatCreateRequest(
        String name,

        String imageUrl,

        @NotEmpty
        Set<UUID> memberIds,

        ChatType type
) {
}
