package ftn.svt.config.security;

import ftn.svt.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatSecurity {

    private final ChatRepository chatRepository;


    public boolean canAccessChat(Authentication authentication, UUID chatId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();

        return chatRepository.existsByIdAndMembersUserUsername(chatId, username);
    }

}

