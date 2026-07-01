package ftn.svt.controller;

import ftn.svt.config.security.ChatSecurity;
import ftn.svt.model.Chat;
import ftn.svt.model.MessageReceipt;
import ftn.svt.model.dto.chat.ChatEventResponse;
import ftn.svt.model.dto.chat.ChatMemberAddRequest;
import ftn.svt.model.dto.chat.ChatMemberRoleUpdateRequest;
import ftn.svt.model.dto.chat.ChatCreateRequest;
import ftn.svt.model.dto.chat.ChatInfoResponse;
import ftn.svt.model.dto.chat.ChatInviteLinkResponse;
import ftn.svt.model.dto.chat.ChatUpdateRequest;
import ftn.svt.model.dto.chat.MessageReceiptResponse;
import ftn.svt.model.dto.chat.MessageStarUpdateResponse;
import ftn.svt.model.dto.chat.MessageStatusResponse;
import ftn.svt.model.dto.chat.StarredMessageResponse;
import ftn.svt.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

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

    @GetMapping("/me/starred-messages")
    public ResponseEntity<?> getMyStarredMessages(Principal principal) {
        List<StarredMessageResponse> res = chatService.getStarredMessages(principal.getName());
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
            @PathVariable UUID chatId,
            Principal principal
    ) {
        return ResponseEntity.ok(chatService.getMessagesByChatId(chatId, principal.getName()));
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
    @PatchMapping("/{chatId}/messages/{messageId}/star")
    public ResponseEntity<?> toggleStarredMessage(
            @PathVariable UUID chatId,
            @PathVariable UUID messageId,
            Principal principal
    ) {
        MessageStarUpdateResponse res = chatService.toggleStarredMessage(
                principal.getName(),
                chatId,
                messageId
        );

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canAccessChat(authentication, #chatId)")
    @PatchMapping("/{chatId}/read")
    public ResponseEntity<?> markChatAsRead(
            @PathVariable UUID chatId,
            Principal principal
    ) {
        List<MessageStatusResponse> messageStatuses = chatService.markChatAsRead(chatId, principal);

        if (!messageStatuses.isEmpty()) {
            Collection<String> memberUsernames = chatService.getChatMemberUsernames(chatId);

            for (String username : memberUsernames) {
                ChatEventResponse event = new ChatEventResponse(
                        "MESSAGE_STATUSES_UPDATED",
                        chatId,
                        null,
                        chatService.getUnreadCount(chatId, username),
                        messageStatuses
                );

                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/chat-events",
                        event
                );
            }
        }

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@chatSecurity.canManageGroup(authentication, #chatId)")
    @PatchMapping("/{chatId}/group")
    public ResponseEntity<?> updateGroup(
            @PathVariable UUID chatId,
            @Valid @RequestBody ChatUpdateRequest dto,
            Principal principal
    ) {
        Chat chat = chatService.updateGroup(chatId, dto, principal.getName());
        var res = ChatInfoResponse.from(chat, chatService.getUnreadCount(chatId, principal.getName()));

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canManageGroup(authentication, #chatId)")
    @PostMapping("/{chatId}/members")
    public ResponseEntity<?> addGroupMembers(
            @PathVariable UUID chatId,
            @Valid @RequestBody ChatMemberAddRequest dto,
            Principal principal
    ) {
        Chat chat = chatService.addGroupMembers(chatId, dto, principal.getName());
        var res = ChatInfoResponse.from(chat, chatService.getUnreadCount(chatId, principal.getName()));

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canManageGroup(authentication, #chatId)")
    @DeleteMapping("/{chatId}/members/{memberId}")
    public ResponseEntity<?> removeGroupMember(
            @PathVariable UUID chatId,
            @PathVariable UUID memberId,
            Principal principal
    ) {
        Chat chat = chatService.removeGroupMember(chatId, memberId, principal.getName());
        var res = ChatInfoResponse.from(chat, chatService.getUnreadCount(chatId, principal.getName()));

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canManageGroup(authentication, #chatId)")
    @PatchMapping("/{chatId}/members/{memberId}/role")
    public ResponseEntity<?> updateGroupMemberRole(
            @PathVariable UUID chatId,
            @PathVariable UUID memberId,
            @Valid @RequestBody ChatMemberRoleUpdateRequest dto,
            Principal principal
    ) {
        Chat chat = chatService.updateGroupMemberRole(chatId, memberId, dto, principal.getName());
        var res = ChatInfoResponse.from(chat, chatService.getUnreadCount(chatId, principal.getName()));

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canManageGroup(authentication, #chatId)")
    @GetMapping("/{chatId}/invite-link")
    public ResponseEntity<?> getGroupInviteLink(
            @PathVariable UUID chatId,
            Principal principal
    ) {
        ChatInviteLinkResponse res = chatService.getGroupInviteLink(chatId, principal.getName());

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canManageGroup(authentication, #chatId)")
    @PostMapping("/{chatId}/invite-link")
    public ResponseEntity<?> generateGroupInviteLink(
            @PathVariable UUID chatId,
            Principal principal
    ) {
        ChatInviteLinkResponse res = chatService.generateGroupInviteLink(chatId, principal.getName());

        return ResponseEntity.ok(res);
    }

    @PreAuthorize("@chatSecurity.canManageGroup(authentication, #chatId)")
    @DeleteMapping("/{chatId}/invite-link")
    public ResponseEntity<?> revokeGroupInviteLink(
            @PathVariable UUID chatId,
            Principal principal
    ) {
        chatService.revokeGroupInviteLink(chatId, principal.getName());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite-links/{token}/join")
    public ResponseEntity<?> joinGroupByInviteLink(
            @PathVariable String token,
            Principal principal
    ) {
        Chat chat = chatService.joinGroupByInviteToken(token, principal.getName());
        var res = ChatInfoResponse.from(
                chat,
                chatService.getUnreadCount(chat.getId(), principal.getName())
        );

        return ResponseEntity.ok(res);
    }
}
