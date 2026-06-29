package ftn.svt.model.dto.chat;

import ftn.svt.model.ChatMember;
import ftn.svt.model.ChatRole;

import java.util.UUID;

public record ChatMemberInfoResponse(
        UUID memberId,
        UUID userId,
        String username,
        String firstName,
        String lastName,
        String fullName,
        String pfpUrl,
        String status,
        String aboutMe,
        ChatRole role
) {
    public static ChatMemberInfoResponse from(ChatMember member) {
        return new ChatMemberInfoResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getFirstName(),
                member.getUser().getLastName(),
                member.getUser().getFullName(),
                member.getUser().getPfpUrl(),
                member.getUser().getStatus(),
                member.getUser().getAboutMe(),
                member.getRole()
        );
    }
}


