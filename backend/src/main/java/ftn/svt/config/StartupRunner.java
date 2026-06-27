package ftn.svt.config;

import ftn.svt.model.*;
import ftn.svt.repository.ChatRepository;
import ftn.svt.repository.MessageReceiptRepository;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChatRepository chatRepository;
    private final MessageReceiptRepository messageReceiptRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        User admin = User.builder()
                .id(null)
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .firstName("Admin")
                .lastName("Adminovic")
                .phoneNumber("+381600000000")
                .email("admin@admin.com")
                .pfpUrl("https://static.vecteezy.com/system/resources/thumbnails/019/194/935/small/global-admin-icon-color-outline-vector.jpg")
                .enabled(true)
                .createdAt(null)
                .build();

        User savedAdmin = userRepository.save(admin);
        System.out.println(savedAdmin);


        if (userRepository.findByUsername("lazarb").isPresent()) {
            return;
        }

        User user1 = User.builder()
                .id(null)
                .username("user")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .firstName("Branko")
                .lastName("Brankovic")
                .phoneNumber("+381611111111")
                .email("branko@brankovic.com")
                .pfpUrl("https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg")
                .enabled(true)
                .createdAt(null)
                .build();

        User user2 = User.builder()
                .id(null)
                .username("luka")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .firstName("Luka")
                .lastName("Lukic")
                .phoneNumber("+381622222222")
                .email("luka@lukic.com")
                .pfpUrl("https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg")
                .enabled(true)
                .createdAt(null)
                .build();

        User user3 = User.builder()
                .id(null)
                .username("jovana")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .firstName("Jovana")
                .lastName("Jovanovic")
                .phoneNumber("+381633333333")
                .email("jovana@jovanovic.com")
                .pfpUrl("https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg")
                .enabled(true)
                .createdAt(null)
                .build();

        User user4 = User.builder()
                .id(null)
                .username("mile")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .firstName("Milan")
                .lastName("Milanovic")
                .phoneNumber("+381644444444")
                .email("milan@milanovic.com")
                .pfpUrl("https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg")
                .enabled(false)
                .blockType(UserBlockType.TEMPORARY)
                .blockReason("Seed account is blocked for moderation review")
                .blockedAt(Instant.now())
                .createdAt(null)
                .build();

        userRepository.saveAll(
                List.of(user1, user2, user3, user4)
        );

        initChat(admin, user2);
    }

    private void initChat(User user1, User user2) {

        Chat chat = Chat.builder()
                .id(null)
                .name(null)
                .imageUrl(null)
                .members(null)
                .messages(null)
                .createdAt(Instant.now())
                .type(ChatType.DIRECT)
                .build();

        ChatMember member1 = ChatMember.builder()
                .id(null)
                .chat(chat)
                .user(user1)
                .role(ChatRole.ADMIN)
                .build();

        ChatMember member2 = ChatMember.builder()
                .id(null)
                .chat(chat)
                .user(user2)
                .role(ChatRole.MEMBER)
                .build();

        Message m1 = Message.builder()
                .id(null)
                .sender(member1)
                .content("Hello, test from " + member1.getUser().getUsername())
                .chat(chat)
                .replyTo(null)
                .forwardedFrom(null)
                .createdAt(null)
                .build();

        Message m2 = Message.builder()
                .id(null)
                .sender(member2)
                .content("Hi, test from " + member2.getUser().getUsername())
                .chat(chat)
                .replyTo(null)
                .forwardedFrom(null)
                .createdAt(null)
                .build();

        Message m3 = Message.builder()
                .id(null)
                .sender(member2)
                .content("Two in a row, test from " + member2.getUser().getUsername())
                .chat(chat)
                .replyTo(null)
                .forwardedFrom(null)
                .createdAt(null)
                .build();

        Message m4 = Message.builder()
                .id(null)
                .sender(member1)
                .content("Last one, test from " + member1.getUser().getUsername())
                .chat(chat)
                .replyTo(null)
                .forwardedFrom(null)
                .createdAt(null)
                .build();

        MessageReceipt mr1 = MessageReceipt.builder()
                .id(null)
                .message(m1)
                .recipient(member2)
                .status(ReceiptStatus.READ)
                .deliveredAt(Instant.now().minusSeconds(5))
                .readAt(Instant.now())
                .build();
        MessageReceipt mr2 = MessageReceipt.builder()
                .id(null)
                .message(m2)
                .recipient(member1)
                .status(ReceiptStatus.READ)
                .deliveredAt(Instant.now().minusSeconds(5))
                .readAt(Instant.now())
                .build();
        MessageReceipt mr3 = MessageReceipt.builder()
                .id(null)
                .message(m3)
                .recipient(member1)
                .status(ReceiptStatus.READ)
                .deliveredAt(Instant.now().minusSeconds(5))
                .readAt(Instant.now())
                .build();
        MessageReceipt mr4 = MessageReceipt.builder()
                .id(null)
                .message(m4)
                .recipient(member2)
                .status(ReceiptStatus.DELIVERED)
                .deliveredAt(Instant.now().minusSeconds(5))
                .readAt(null)
                .build();

        chat.setMembers(List.of(member1, member2));
        chat.setMessages(List.of(m1, m2, m3, m4));

        chatRepository.save(chat);
        messageReceiptRepository.saveAll(List.of(mr1, mr2, mr3, mr4));
    }
}
