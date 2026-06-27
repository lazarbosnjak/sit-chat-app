package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.*;
import ftn.svt.model.dto.chat.ChatCreateRequest;
import ftn.svt.model.dto.chat.ChatInfoResponse;
import ftn.svt.model.dto.chat.MessageResponse;
import ftn.svt.model.dto.chat.MessageStatusResponse;
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

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesByChatId(UUID chatId) {
        List<Message> messages = messageRepository.findAllByChatId(chatId);
        Map<UUID, ReceiptStatus> deliveryStatuses = getDeliveryStatuses(messages);

        return messages.stream()
                .map(message -> MessageResponse.from(
                        message,
                        deliveryStatuses.getOrDefault(message.getId(), ReceiptStatus.SENT)
                ))
                .toList();
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
    public List<MessageStatusResponse> markChatAsRead(UUID chatId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        List<MessageReceipt> unreadReceipts = messageReceiptRepository
                .findUnreadByChatIdAndUserId(chatId, user.getId());

        if (unreadReceipts.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();

        unreadReceipts.forEach(receipt -> {
            receipt.setStatus(ReceiptStatus.READ);
            receipt.setReadAt(now);

            if (receipt.getDeliveredAt() == null) {
                receipt.setDeliveredAt(now);
            }
        });

        messageReceiptRepository.saveAll(unreadReceipts);

        return getStatusUpdatesForMessages(unreadReceipts.stream()
                .map(receipt -> receipt.getMessage().getId())
                .collect(Collectors.toSet()));
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
                            .status(isSender ? ReceiptStatus.READ : ReceiptStatus.SENT)
                            .deliveredAt(isSender ? now : null)
                            .readAt(isSender ? now : null)
                            .build();
                })
                .toList();

        messageReceiptRepository.saveAll(receipts);

        return MessageResponse.from(savedMessage, getDeliveryStatus(receipts));
    }

    @Transactional
    public MessageStatusResponse markMessageAsDelivered(UUID messageId) {
        messageReceiptRepository.markMessageAsDelivered(
                messageId,
                ReceiptStatus.SENT,
                ReceiptStatus.DELIVERED,
                Instant.now()
        );

        List<MessageReceipt> receipts = messageReceiptRepository.findByMessageId(messageId);
        return new MessageStatusResponse(messageId, getDeliveryStatus(receipts));
    }

    @Transactional(readOnly = true)
    public Collection<String> getChatMemberUsernames(UUID chatId) {
        return chatMemberRepository.findUsernamesByChatId(chatId);
    }

    private List<MessageStatusResponse> getStatusUpdatesForMessages(Collection<UUID> messageIds) {
        if (messageIds.isEmpty()) {
            return List.of();
        }

        List<MessageReceipt> receipts = messageReceiptRepository.findByMessageIdIn(messageIds);
        Map<UUID, List<MessageReceipt>> receiptsByMessageId = receipts.stream()
                .collect(Collectors.groupingBy(receipt -> receipt.getMessage().getId()));

        return messageIds.stream()
                .map(messageId -> new MessageStatusResponse(
                        messageId,
                        getDeliveryStatus(receiptsByMessageId.getOrDefault(messageId, List.of()))
                ))
                .toList();
    }

    private Map<UUID, ReceiptStatus> getDeliveryStatuses(List<Message> messages) {
        if (messages.isEmpty()) {
            return Map.of();
        }

        Set<UUID> messageIds = messages.stream()
                .map(Message::getId)
                .collect(Collectors.toSet());

        List<MessageReceipt> receipts = messageReceiptRepository.findByMessageIdIn(messageIds);
        Map<UUID, List<MessageReceipt>> receiptsByMessageId = receipts.stream()
                .collect(Collectors.groupingBy(receipt -> receipt.getMessage().getId()));

        return messages.stream()
                .collect(Collectors.toMap(
                        Message::getId,
                        message -> getDeliveryStatus(
                                receiptsByMessageId.getOrDefault(message.getId(), List.of())
                        )
                ));
    }

    private ReceiptStatus getDeliveryStatus(List<MessageReceipt> receipts) {
        List<MessageReceipt> recipientReceipts = receipts.stream()
                .filter(receipt -> !isSenderReceipt(receipt))
                .toList();

        if (recipientReceipts.isEmpty()) {
            return ReceiptStatus.READ;
        }

        boolean allRead = recipientReceipts.stream()
                .allMatch(receipt -> receipt.getStatus() == ReceiptStatus.READ);

        if (allRead) {
            return ReceiptStatus.READ;
        }

        boolean allDelivered = recipientReceipts.stream()
                .allMatch(receipt -> receipt.getStatus() != ReceiptStatus.SENT);

        return allDelivered ? ReceiptStatus.DELIVERED : ReceiptStatus.SENT;
    }

    private boolean isSenderReceipt(MessageReceipt receipt) {
        return receipt.getRecipient().getId().equals(receipt.getMessage().getSender().getId());
    }
}
