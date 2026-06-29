package ftn.svt.controller;

import ftn.svt.model.dto.chat.ChatEventResponse;
import ftn.svt.model.dto.chat.ChatMessageRequest;
import ftn.svt.model.dto.chat.MessageReactionRequest;
import ftn.svt.model.dto.chat.MessageResponse;
import ftn.svt.model.dto.chat.MessageStatusResponse;
import ftn.svt.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest request, Principal principal) {
        String senderUsername = principal.getName();

        MessageResponse savedMessage =
                chatService.sendMessage(
                        senderUsername,
                        request.chatId(),
                        request.content(),
                        request.replyToMessageId(),
                        request.forwardedFromMessageId()
                );

        Collection<String> memberUsernames =
                chatService.getChatMemberUsernames(request.chatId());

        for (String username : memberUsernames) {
            long unreadCount = chatService.getUnreadCount(request.chatId(), username);

            ChatEventResponse event = new ChatEventResponse(
                    "MESSAGE_CREATED",
                    request.chatId(),
                    savedMessage,
                    unreadCount,
                    List.of()
            );

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/chat-events",
                    event
            );

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/messages",
                    savedMessage
            );
        }

        MessageStatusResponse deliveredStatus =
                chatService.markMessageAsDelivered(savedMessage.id());

        for (String username : memberUsernames) {
            ChatEventResponse event = new ChatEventResponse(
                    "MESSAGE_STATUSES_UPDATED",
                    request.chatId(),
                    null,
                    chatService.getUnreadCount(request.chatId(), username),
                    List.of(deliveredStatus)
            );

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/chat-events",
                    event
            );
        }
    }

    @MessageMapping("/chat.react")
    public void reactToMessage(MessageReactionRequest request, Principal principal) {
        UUID chatId = chatService.reactToMessage(
                principal.getName(),
                request.messageId(),
                request.type()
        );

        Collection<String> memberUsernames =
                chatService.getChatMemberUsernames(chatId);

        for (String username : memberUsernames) {
            ChatEventResponse event = new ChatEventResponse(
                    "MESSAGE_REACTIONS_UPDATED",
                    chatId,
                    null,
                    chatService.getUnreadCount(chatId, username),
                    List.of(),
                    request.messageId(),
                    chatService.getReactionSummariesForMessage(request.messageId(), username)
            );

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/chat-events",
                    event
            );
        }
    }
}
