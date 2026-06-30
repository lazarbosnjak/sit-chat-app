package ftn.svt.model.dto.chat;

import ftn.svt.model.ChatRole;
import jakarta.validation.constraints.NotNull;

public record ChatMemberRoleUpdateRequest(
        @NotNull
        ChatRole role
) {
}
