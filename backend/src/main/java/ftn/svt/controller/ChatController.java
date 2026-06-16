package ftn.svt.controller;

import ftn.svt.model.Chat;
import ftn.svt.model.dto.chat.ChatCreateRequest;
import ftn.svt.model.dto.chat.ChatInfoResponse;
import ftn.svt.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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

}
