package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.*;
import ftn.svt.model.dto.chat.ChatCreateRequest;
import ftn.svt.model.dto.chat.ChatInfoResponse;
import ftn.svt.model.dto.chat.MessageResponse;
import ftn.svt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final ChatMemberRepository chatMemberRepository;

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

    public Collection<ChatInfoResponse> getAllByPrincipal(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Collection<Chat> chats = chatRepository.findAllWithUserId(user.getId());

        Map<UUID, Long> unreadCounts = messageReceiptRepository
                .countUnreadByChatForUser(user.getId())
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        return chats.stream()
                .map(chat -> ChatInfoResponse.from(
                        chat,
                        unreadCounts.getOrDefault(chat.getId(), 0L)
                ))
                .toList();
    }

    public Chat getById(UUID id) {
        return chatRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Chat with this id does not exist"));
    }

    public List<Message> getMessagesByChatId(UUID chatId) {
        return messageRepository.findAllByChatId(chatId);
    }

    public List<MessageReceipt> getMessageReceiptsByChatId(UUID chatId) {
        return messageReceiptRepository.findByMessageChatId(chatId);
    }

    public long getUnreadCount(UUID chatId, String username) {
        User user = userService.findByUsername(username);

        return messageReceiptRepository.countUnreadByChatIdAndUserId(
                chatId,
                user.getId()
        );
    }

    @Transactional
    public void markChatAsRead(UUID chatId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        messageReceiptRepository.markChatAsRead(
                chatId,
                user.getId(),
                ReceiptStatus.READ,
                Instant.now()
        );
    }

    @Transactional
    public MessageResponse sendMessage(String senderUsername, UUID chatId, String content) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        if (!sender.isEnabled()) {
            throw ApiException.forbidden("Blocked users cannot send messages");
        }

        if (content == null || content.isBlank()) {
            throw ApiException.badRequest("Message content cannot be empty");
        }

        ChatMember senderMember = chatMemberRepository
                .findByChatIdAndUserUsername(chatId, senderUsername)
                .orElseThrow(() -> ApiException.forbidden("You are not a member of this chat"));

        Chat chat = senderMember.getChat();

        Message message = Message.builder()
                .chat(chat)
                .sender(senderMember)
                .content(content.trim())
                .build();

        Message savedMessage = messageRepository.save(message);

        List<ChatMember> recipients = chatMemberRepository.findAllByChatId(chatId);

        Instant now = Instant.now();

        List<MessageReceipt> receipts = recipients.stream()
                .map(recipient -> {
                    boolean isSender = recipient.getId().equals(senderMember.getId());

                    return MessageReceipt.builder()
                            .message(savedMessage)
                            .recipient(recipient)
                            .status(isSender ? ReceiptStatus.READ : ReceiptStatus.DELIVERED)
                            .deliveredAt(now)
                            .readAt(isSender ? now : null)
                            .build();
                })
                .toList();

        messageReceiptRepository.saveAll(receipts);

        return MessageResponse.from(savedMessage);
    }

    @Transactional(readOnly = true)
    public Collection<String> getChatMemberUsernames(UUID chatId) {
        return chatMemberRepository.findUsernamesByChatId(chatId);
    }
}
