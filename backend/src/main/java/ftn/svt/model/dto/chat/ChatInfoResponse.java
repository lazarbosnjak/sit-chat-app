package ftn.svt.model.dto.chat;

import ftn.svt.model.Chat;
import ftn.svt.model.ChatType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ChatInfoResponse(
        UUID id,
        String name,
        String imageUrl,
        ChatType type,
        Instant createdAt,
        Set<ChatMemberInfoResponse> members
) {
    public static ChatInfoResponse from(Chat chat) {
        return new ChatInfoResponse(
                chat.getId(),
                chat.getName(),
                chat.getImageUrl(),
                chat.getType(),
                chat.getCreatedAt(),
                chat.getMembers().stream()
                        .map(member -> new ChatMemberInfoResponse(
                                member.getId(),
                                member.getUser().getUsername(),
                                member.getUser().getFullName(),
                                member.getRole()
                        )).collect(Collectors.toSet())
        );
    }
}
