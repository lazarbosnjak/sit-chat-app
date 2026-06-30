package ftn.svt.model.dto.chat;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record ChatMemberAddRequest(
        @NotEmpty
        Set<UUID> memberIds
) {
}
