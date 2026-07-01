package ftn.svt.service;

import ftn.svt.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@sitapp.local}")
    private String fromAddress;

    public void sendRegistrationApprovedEmail(User user) {
        send(
                user.getEmail(),
                "SitApp registration approved",
                """
                        Hello %s,

                        Your SitApp registration request has been approved.
                        You can now sign in with your username: %s
                        """.formatted(user.getFirstName(), user.getUsername())
        );
    }

    public void sendPasswordResetLink(User user, String resetLink, Instant expiresAt) {
        send(
                user.getEmail(),
                "SitApp password reset",
                """
                        Hello %s,

                        Use this link to change your SitApp password:
                        %s

                        This link expires at %s and can be used only once.
                        """.formatted(user.getFirstName(), resetLink, expiresAt)
        );
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
