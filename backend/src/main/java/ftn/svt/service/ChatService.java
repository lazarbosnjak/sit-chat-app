package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.*;
import ftn.svt.model.dto.chat.ChatCreateRequest;
import ftn.svt.repository.ChatRepository;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public Chat create(ChatCreateRequest dto, Principal principal) {
        User initiator = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> ApiException.notFound("user not found"));

        Set<UUID> memberIds = new HashSet<>(dto.memberIds());
        memberIds.add(initiator.getId());

        if (memberIds.size() > 2 && dto.type() == ChatType.DIRECT) {
            throw ApiException.badRequest("Direct messages must have 2 members");
        }

        if (memberIds.size() < 2) {
            throw ApiException.badRequest("Chats must have at least 2 members");
        }

        if (dto.type() == ChatType.DIRECT) {
            chatRepository.findByExactMemberIds(memberIds, memberIds.size())
                    .ifPresent(existingChat -> {
                        throw ApiException.conflict("chat with these members exist");
                    });
        }


        Chat chat = Chat.builder()
                .id(null)
                .name(dto.name())
                .imageUrl(dto.imageUrl())
                .members(new ArrayList<>())
                .createdAt(null)
                .type(dto.type())
                .build();

        Chat savedChat = chatRepository.save(chat);

        Set<User> users = new HashSet<>(userRepository.findAllById(memberIds));

        if (users.size() != memberIds.size()) {
            throw ApiException.notFound("one or more users not found");
        }

        List<ChatMember> members = users.stream()
                .map(user -> ChatMember.builder()
                        .chat(savedChat)
                        .user(user)
                        .role(user.getId().equals(initiator.getId())
                                ? ChatRole.ADMIN
                                : ChatRole.MEMBER)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        savedChat.setMembers(members);
        return chatRepository.save(savedChat);

    }

    public Collection<Chat> getAllByPrincipal(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        return chatRepository.findAllWithUserId(user.getId());
    }

    public Chat getById(UUID id) {
        return chatRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Chat with this id does not exist"));
    }
}
