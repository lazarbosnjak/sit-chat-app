package ftn.svt.controller;

import ftn.svt.model.dto.audio.AudioStreamResponse;
import ftn.svt.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/audio")
@RequiredArgsConstructor
public class AudioController {

    private final ChatService chatService;

    @GetMapping("/{audioId}")
    public ResponseEntity<byte[]> streamAudio(
            @PathVariable UUID audioId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            Principal principal
    ) {
        AudioStreamResponse audio = chatService.streamVoiceMessage(
                principal.getName(),
                audioId,
                rangeHeader
        );

        return ResponseEntity
                .status(audio.statusCode())
                .headers(audio.headers())
                .body(audio.body());
    }

    @DeleteMapping("/{audioId}")
    public ResponseEntity<?> deleteAudio(
            @PathVariable UUID audioId,
            Principal principal
    ) {
        chatService.deleteVoiceMessageAudio(principal.getName(), audioId);

        return ResponseEntity.noContent().build();
    }
}
