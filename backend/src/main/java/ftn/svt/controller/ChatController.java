package ftn.svt.controller;

import ftn.svt.config.security.ChatSecurity;
import ftn.svt.model.Chat;
import ftn.svt.model.Message;
import ftn.svt.model.MessageReceipt;
import ftn.svt.model.dto.chat.ChatCreateRequest;
import ftn.svt.model.dto.chat.ChatInfoResponse;
import ftn.svt.model.dto.chat.MessageReceiptResponse;
import ftn.svt.model.dto.chat.MessageResponse;
import ftn.svt.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatSecurity chatSecurity;

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody ChatCreateRequest dto,
            Principal principal
    ) {
        Chat chat = chatService.create(dto, principal);
        var res = ChatInfoResponse.from(chat, 0);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(res);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMine(Principal principal) {
        Collection<ChatInfoResponse> res = chatService.getAllByPrincipal(principal);
        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canAccessChat(authentication, #id)")
    @GetMapping({"/{id}"})
    public ResponseEntity<?> getById(
            @PathVariable UUID id,
            Principal principal
    ) {
        Chat chat = chatService.getById(id);

        var unreadCount = chatService.getUnreadCount(chat.getId(), principal.getName());
        var res = ChatInfoResponse.from(chat, unreadCount);

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canAccessChat(authentication, #chatId)")
    @GetMapping({"/{chatId}/messages"})
    public ResponseEntity<?> getMessagesByChatId(
            @PathVariable UUID chatId
    ) {
        List<Message> messages = chatService.getMessagesByChatId(chatId);
        var res = messages.stream().map(MessageResponse::from).toList();
        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canAccessChat(authentication, #chatId)")
    @GetMapping("/{chatId}/message-receipts")
    public ResponseEntity<?> getMessageReceiptsByChatId(@PathVariable UUID chatId) {
        List<MessageReceipt> receipts = chatService.getMessageReceiptsByChatId(chatId);

        var res = receipts.stream()
                .map(MessageReceiptResponse::from)
                .toList();

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canAccessChat(authentication, #chatId)")
    @PatchMapping("/{chatId}/read")
    public ResponseEntity<?> markChatAsRead(
            @PathVariable UUID chatId,
            Principal principal
    ) {
        chatService.markChatAsRead(chatId, principal);
        return ResponseEntity.noContent().build();
    }
}
