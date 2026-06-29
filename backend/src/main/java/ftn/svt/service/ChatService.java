package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.*;
import ftn.svt.model.dto.chat.ChatCreateRequest;
import ftn.svt.model.dto.chat.ChatInfoResponse;
import ftn.svt.model.dto.chat.MessageReactionSummaryResponse;
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
    private final MessageReactionRepository messageReactionRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserActivityService userActivityService;

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
        Chat createdChat = chatRepository.save(savedChat);
        userActivityService.recordActivity(initiator.getId(), UserActivityType.CHAT_CREATED);

        return createdChat;

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
    public List<MessageResponse> getMessagesByChatId(UUID chatId, String viewerUsername) {
        List<Message> messages = messageRepository.findAllByChatIdOrderByCreatedAtAsc(chatId);
        Map<UUID, ReceiptStatus> deliveryStatuses = getDeliveryStatuses(messages);
        Map<UUID, List<MessageReactionSummaryResponse>> reactionSummaries =
                getReactionSummaries(messages, viewerUsername);

        return messages.stream()
                .map(message -> MessageResponse.from(
                        message,
                        deliveryStatuses.getOrDefault(message.getId(), ReceiptStatus.SENT),
                        reactionSummaries.getOrDefault(message.getId(), List.of())
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
        userActivityService.recordActivity(user.getId(), UserActivityType.MESSAGE_READ);

        return getStatusUpdatesForMessages(unreadReceipts.stream()
                .map(receipt -> receipt.getMessage().getId())
                .collect(Collectors.toSet()));
    }

    @Transactional
    public MessageResponse sendMessage(
            String senderUsername,
            UUID chatId,
            String content,
            UUID replyToMessageId,
            UUID forwardedFromMessageId
    ) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        if (!sender.isEnabled()) {
            throw ApiException.forbidden("Blocked users cannot send messages");
        }

        ChatMember senderMember = chatMemberRepository
                .findByChatIdAndUserUsername(chatId, senderUsername)
                .orElseThrow(() -> ApiException.forbidden("You are not a member of this chat"));

        Chat chat = senderMember.getChat();
        Message replyTo = getReplyToMessage(replyToMessageId, chatId);
        Message forwardedFrom = getForwardedFromMessage(forwardedFromMessageId, senderUsername);
        String normalizedContent = normalizeMessageContent(content, forwardedFrom);

        if (normalizedContent.isBlank()) {
            throw ApiException.badRequest("Message content cannot be empty");
        }

        Message message = Message.builder()
                .chat(chat)
                .sender(senderMember)
                .replyTo(replyTo)
                .forwardedFrom(forwardedFrom)
                .content(normalizedContent)
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
        userActivityService.recordActivity(sender.getId(), UserActivityType.MESSAGE_SENT);

        return MessageResponse.from(savedMessage, getDeliveryStatus(receipts), List.of());
    }

    @Transactional
    public UUID reactToMessage(String username, UUID messageId, MessageReactionType type) {
        if (messageId == null) {
            throw ApiException.badRequest("Message id is required");
        }

        if (type == null) {
            throw ApiException.badRequest("Reaction type is required");
        }

        User reactor = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        if (!reactor.isEnabled()) {
            throw ApiException.forbidden("Blocked users cannot react to messages");
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Message with this id does not exist"));

        UUID chatId = message.getChat().getId();
        ChatMember reactorMember = chatMemberRepository
                .findByChatIdAndUserUsername(chatId, username)
                .orElseThrow(() -> ApiException.forbidden("You are not a member of this chat"));

        Optional<MessageReaction> existingReaction =
                messageReactionRepository.findByMessageIdAndReactorId(messageId, reactorMember.getId());

        if (existingReaction.isPresent()) {
            MessageReaction reaction = existingReaction.get();

            if (reaction.getType() == type) {
                messageReactionRepository.delete(reaction);
                return chatId;
            }

            reaction.setType(type);
            messageReactionRepository.save(reaction);
            return chatId;
        }

        MessageReaction reaction = MessageReaction.builder()
                .message(message)
                .reactor(reactorMember)
                .type(type)
                .build();

        messageReactionRepository.save(reaction);
        return chatId;
    }

    @Transactional(readOnly = true)
    public List<MessageReactionSummaryResponse> getReactionSummariesForMessage(
            UUID messageId,
            String viewerUsername
    ) {
        return summarizeReactions(
                messageReactionRepository.findByMessageId(messageId),
                viewerUsername
        );
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

    private Message getReplyToMessage(UUID replyToMessageId, UUID chatId) {
        if (replyToMessageId == null) {
            return null;
        }

        Message replyTo = messageRepository.findById(replyToMessageId)
                .orElseThrow(() -> ApiException.notFound("Reply message with this id does not exist"));

        if (!replyTo.getChat().getId().equals(chatId)) {
            throw ApiException.badRequest("Reply message must belong to the same chat");
        }

        return replyTo;
    }

    private Message getForwardedFromMessage(UUID forwardedFromMessageId, String senderUsername) {
        if (forwardedFromMessageId == null) {
            return null;
        }

        Message forwardedFrom = messageRepository.findById(forwardedFromMessageId)
                .orElseThrow(() -> ApiException.notFound("Forwarded message with this id does not exist"));

        boolean canAccessSourceChat = chatRepository.existsByIdAndMembersUserUsername(
                forwardedFrom.getChat().getId(),
                senderUsername
        );

        if (!canAccessSourceChat) {
            throw ApiException.forbidden("You cannot forward a message from a chat you cannot access");
        }

        return forwardedFrom;
    }

    private String normalizeMessageContent(String content, Message forwardedFrom) {
        String normalizedContent = content != null ? content.trim() : "";

        if (normalizedContent.isBlank() && forwardedFrom != null) {
            return forwardedFrom.getContent();
        }

        return normalizedContent;
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

    private Map<UUID, List<MessageReactionSummaryResponse>> getReactionSummaries(
            List<Message> messages,
            String viewerUsername
    ) {
        if (messages.isEmpty()) {
            return Map.of();
        }

        Set<UUID> messageIds = messages.stream()
                .map(Message::getId)
                .collect(Collectors.toSet());

        List<MessageReaction> reactions = messageReactionRepository.findByMessageIdIn(messageIds);
        Map<UUID, List<MessageReaction>> reactionsByMessageId = reactions.stream()
                .collect(Collectors.groupingBy(reaction -> reaction.getMessage().getId()));

        return messages.stream()
                .collect(Collectors.toMap(
                        Message::getId,
                        message -> summarizeReactions(
                                reactionsByMessageId.getOrDefault(message.getId(), List.of()),
                                viewerUsername
                        )
                ));
    }

    private List<MessageReactionSummaryResponse> summarizeReactions(
            List<MessageReaction> reactions,
            String viewerUsername
    ) {
        if (reactions.isEmpty()) {
            return List.of();
        }

        Map<MessageReactionType, Long> countsByType = reactions.stream()
                .collect(Collectors.groupingBy(
                        MessageReaction::getType,
                        () -> new EnumMap<>(MessageReactionType.class),
                        Collectors.counting()
                ));

        Set<MessageReactionType> viewerReactionTypes = reactions.stream()
                .filter(reaction -> reaction.getReactor().getUser().getUsername().equals(viewerUsername))
                .map(MessageReaction::getType)
                .collect(Collectors.toSet());

        return Arrays.stream(MessageReactionType.values())
                .filter(type -> countsByType.containsKey(type) || viewerReactionTypes.contains(type))
                .map(type -> new MessageReactionSummaryResponse(
                        type,
                        countsByType.getOrDefault(type, 0L),
                        viewerReactionTypes.contains(type)
                ))
                .toList();
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
