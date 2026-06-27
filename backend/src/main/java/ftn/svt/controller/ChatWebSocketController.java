package ftn.svt.controller;

import ftn.svt.model.dto.chat.ChatEventResponse;
import ftn.svt.model.dto.chat.ChatMessageRequest;
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

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest request, Principal principal) {
        String senderUsername = principal.getName();

        MessageResponse savedMessage =
                chatService.sendMessage(senderUsername, request.chatId(), request.content());

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
}
