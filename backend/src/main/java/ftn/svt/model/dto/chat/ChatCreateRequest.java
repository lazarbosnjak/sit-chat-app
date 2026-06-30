package ftn.svt.model.dto.chat;

import ftn.svt.model.ChatType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record ChatCreateRequest(
        String name,

        String description,

        String imageUrl,

        @NotEmpty
        Set<UUID> memberIds,

        @NotNull
        ChatType type
) {
}
