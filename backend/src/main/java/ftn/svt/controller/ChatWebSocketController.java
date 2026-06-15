package ftn.svt.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public String sendMessage(String message, Principal principal) {
        String username = principal != null ? principal.getName() : "anon";

        return username + ": " + message;
    }
}
