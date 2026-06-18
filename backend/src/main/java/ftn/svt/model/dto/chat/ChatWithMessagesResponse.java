package ftn.svt.model.dto.chat;

import ftn.svt.model.ChatType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ChatWithMessagesResponse(
        UUID id,
        String name,
        String imageUrl,
        ChatType type,
        Instant createdAt,
        List<MessageResponse> messages,
        Set<ChatMemberInfoResponse>members
) {
//    public static ChatWithMessagesResponse from(Chat chat) {
//        return new ChatWithMessagesResponse(
//                chat.getId(),
//                chat.getName(),
//                chat.getImageUrl(),
//                chat.getType(),
//                chat.getCreatedAt(),
//                chat.getMessages().stream()
//                        .map(msg -> new ChatMessageResponse(
//                                msg.getId(),
//                                msg.getSender().getId(),
//                                msg.getContent(),
//                                msg.getReplyTo() != null
//                                    ? msg.getReplyTo().getId()
//                                    : null,
//                                msg.getForwardedFrom() != null
//                                    ? msg.getForwardedFrom().getId()
//                                    : null,
//                                msg.getCreatedAt()
//                        )).toList(),
//                chat.getMembers().stream()
//                        .map(member -> new ChatMemberInfoResponse(
//                                member.getId(),
//                                member.getUser().getId(),
//                                member.getUser().getUsername(),
//                                member.getUser().getFullName(),
//                                member.getUser().getPfpUrl(),
//                                member.getRole()
//                        )).collect(Collectors.toSet())
//        );
//    }
}
