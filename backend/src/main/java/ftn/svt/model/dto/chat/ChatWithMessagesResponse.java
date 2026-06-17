package ftn.svt.model.dto.chat;

import ftn.svt.model.Chat;
import ftn.svt.model.ChatType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ChatWithMessagesResponse(
        UUID id,
        String name,
        String imageUrl,
        ChatType type,
        Instant createdAt,
        List<ChatMessageResponse> messages,
        Set<ChatMemberInfoResponse>members
) {
    public static ChatWithMessagesResponse from(Chat chat) {
        return new ChatWithMessagesResponse(
                chat.getId(),
                chat.getName(),
                chat.getImageUrl(),
                chat.getType(),
                chat.getCreatedAt(),
                chat.getMessages().stream()
                        .map(msg -> new ChatMessageResponse(
                                msg.getId(),
                                msg.getSender().getId(),
                                msg.getContent(),
                                msg.getReplyTo().getId(),
                                msg.getForwardedFrom().getId(),
                                msg.getCreatedAt()
                        )).toList(),
                chat.getMembers().stream()
                        .map(member -> new ChatMemberInfoResponse(
                                member.getId(),
                                member.getUser().getId(),
                                member.getUser().getUsername(),
                                member.getUser().getFullName(),
                                member.getUser().getPfpUrl(),
                                member.getRole()
                        )).collect(Collectors.toSet())
        );
    }
}
