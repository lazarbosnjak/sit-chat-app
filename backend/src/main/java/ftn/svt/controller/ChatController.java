package ftn.svt.controller;

import ftn.svt.model.Chat;
import ftn.svt.model.dto.chat.ChatCreateRequest;
import ftn.svt.model.dto.chat.ChatInfoResponse;
import ftn.svt.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;

@RestController
@RequestMapping("/api/v0/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody ChatCreateRequest dto,
            Principal principal
    ) {
        Chat chat = chatService.create(dto, principal);
        var res = ChatInfoResponse.from(chat);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(res);
    }

    // TODO: Add interceptor that only allows people in the chat to request this
    @GetMapping("/me")
    public ResponseEntity<?> getMine(Principal principal) {
        Collection<Chat> chats = chatService.getAllByPrincipal(principal);
        var res = chats.stream().map(ChatInfoResponse::from).toList();
        return ResponseEntity.ok(res);
    }

}
