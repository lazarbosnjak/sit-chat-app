package ftn.svt.config.security;

import ftn.svt.repository.ChatRepository;
import ftn.svt.repository.ChatMemberRepository;
import ftn.svt.model.ChatRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatSecurity {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;


    public boolean canAccessChat(Authentication authentication, UUID chatId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();

        return chatRepository.existsActiveByIdAndMemberUsername(chatId, username);
    }

    public boolean canManageGroup(Authentication authentication, UUID chatId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return chatMemberRepository.existsByChatIdAndUserUsernameAndRoleAndActiveTrue(
                chatId,
                authentication.getName(),
                ChatRole.ADMIN
        );
    }
}
