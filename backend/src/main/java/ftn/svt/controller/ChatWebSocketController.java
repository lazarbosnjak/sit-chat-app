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

//    @MessageMapping("/chat.send")
//    public void sendMessage(ChatMessageRequest request, Principal principal) {
//        String senderUsername = principal.getName();
//
//        ChatMessageResponse message = chatService.saveAndBuildMessage(
//                senderUsername,
//                request.recipientUsername(),
//                request.content()
//        );
//
//        messagingTemplate.convertAndSendToUser(
//                request.recipientUsername(),
//                "/queue/messages",
//                message
//        );
//
//        messagingTemplate.convertAndSendToUser(
//                senderUsername,
//                "/queue/messages",
//                message
//        );
//    }
}
