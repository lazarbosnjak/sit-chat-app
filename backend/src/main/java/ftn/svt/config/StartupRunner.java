package ftn.svt.config;

import ftn.svt.model.*;
import ftn.svt.repository.ChatRepository;
import ftn.svt.repository.MessageReceiptRepository;
import ftn.svt.repository.UserActivityRepository;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChatRepository chatRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final UserActivityRepository userActivityRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        Instant now = Instant.now();

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
                .status("Monitoring the system")
                .aboutMe("Seed administrator account for analytics and moderation demos.")
                .enabled(true)
                .createdAt(daysAgo(now, 28))
                .lastActiveAt(hoursAgo(now, 1))
                .build();

        User savedAdmin = saveUserWithCreatedAt(admin);
        System.out.println(savedAdmin);

        User user1 = User.builder()
                .id(null)
                .username("user")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .firstName("Branko")
                .lastName("Brankovic")
                .phoneNumber("+381611111111")
                .email("branko@brankovic.com")
                .pfpUrl(User.DEFAULT_PROFILE_PICTURE_URL)
                .status("Available")
                .aboutMe("Backend developer and coffee enthusiast.")
                .enabled(true)
                .createdAt(daysAgo(now, 24))
                .lastActiveAt(hoursAgo(now, 4))
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
                .pfpUrl(User.DEFAULT_PROFILE_PICTURE_URL)
                .status("Shipping chat features")
                .aboutMe("Likes product discussions and group chats.")
                .enabled(true)
                .createdAt(daysAgo(now, 19))
                .lastActiveAt(hoursAgo(now, 8))
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
                .pfpUrl(User.DEFAULT_PROFILE_PICTURE_URL)
                .status("Reviewing designs")
                .aboutMe("Focused on UX details and release planning.")
                .enabled(true)
                .createdAt(daysAgo(now, 13))
                .lastActiveAt(daysAgo(now, 1))
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
                .pfpUrl(User.DEFAULT_PROFILE_PICTURE_URL)
                .status("Temporarily unavailable")
                .aboutMe("Seed user with a temporary moderation block.")
                .enabled(false)
                .blockType(UserBlockType.TEMPORARY)
                .blockReason("Seed account is blocked for moderation review")
                .blockedAt(daysAgo(now, 2))
                .createdAt(daysAgo(now, 8))
                .lastActiveAt(daysAgo(now, 5))
                .build();

        User user5 = User.builder()
                .id(null)
                .username("ana")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .firstName("Ana")
                .lastName("Anic")
                .phoneNumber("+381655555555")
                .email("ana@anic.com")
                .pfpUrl(User.DEFAULT_PROFILE_PICTURE_URL)
                .status("Planning sprint tasks")
                .aboutMe("Project coordinator for seed group conversations.")
                .enabled(true)
                .createdAt(daysAgo(now, 5))
                .lastActiveAt(hoursAgo(now, 2))
                .build();

        User user6 = User.builder()
                .id(null)
                .username("marko")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .firstName("Marko")
                .lastName("Markovic")
                .phoneNumber("+381666666666")
                .email("marko@markovic.com")
                .pfpUrl(User.DEFAULT_PROFILE_PICTURE_URL)
                .status("Testing notifications")
                .aboutMe("QA-focused seed account.")
                .enabled(true)
                .createdAt(daysAgo(now, 3))
                .lastActiveAt(hoursAgo(now, 6))
                .build();

        List<User> savedUsers = saveUsersWithCreatedAt(
                List.of(user1, user2, user3, user4, user5, user6)
        );

        User savedUser1 = savedUsers.get(0);
        User savedUser2 = savedUsers.get(1);
        User savedUser3 = savedUsers.get(2);
        User savedUser4 = savedUsers.get(3);
        User savedUser5 = savedUsers.get(4);
        User savedUser6 = savedUsers.get(5);

        initDirectChat(savedAdmin, savedUser2, hoursAgo(now, 2));
        initDirectChat(savedUser1, savedUser3, now.minus(3, ChronoUnit.DAYS));
        initGroupChat(
                "SVT Project Team",
                "Daily coordination for the SVT project.",
                "https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=300&q=80",
                now.minus(18, ChronoUnit.DAYS),
                List.of(savedAdmin, savedUser1, savedUser2, savedUser3)
        );
        initGroupChat(
                "QA Lab",
                "Testing scenarios, bugs, and release checks.",
                "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=300&q=80",
                now.minus(11, ChronoUnit.DAYS),
                List.of(savedUser2, savedUser3, savedUser5, savedUser6)
        );
        initGroupChat(
                "Release Planning",
                "Launch preparation and final sign-off.",
                "https://images.unsplash.com/photo-1551434678-e076c223a692?auto=format&fit=crop&w=300&q=80",
                now.minus(4, ChronoUnit.DAYS),
                List.of(savedAdmin, savedUser1, savedUser5, savedUser6)
        );

        initUserActivities(
                now,
                List.of(savedAdmin, savedUser1, savedUser2, savedUser3, savedUser4, savedUser5, savedUser6)
        );
    }

    private void initDirectChat(User user1, User user2, Instant createdAt) {

        Chat chat = Chat.builder()
                .id(null)
                .name(null)
                .description(null)
                .imageUrl(null)
                .members(null)
                .messages(null)
                .createdAt(createdAt)
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
                .createdAt(createdAt.plus(10, ChronoUnit.MINUTES))
                .build();

        Message m2 = Message.builder()
                .id(null)
                .sender(member2)
                .content("Hi, test from " + member2.getUser().getUsername())
                .chat(chat)
                .replyTo(null)
                .forwardedFrom(null)
                .createdAt(createdAt.plus(14, ChronoUnit.MINUTES))
                .build();

        Message m3 = Message.builder()
                .id(null)
                .sender(member2)
                .content("Two in a row, test from " + member2.getUser().getUsername())
                .chat(chat)
                .replyTo(null)
                .forwardedFrom(null)
                .createdAt(createdAt.plus(18, ChronoUnit.MINUTES))
                .build();

        Message m4 = Message.builder()
                .id(null)
                .sender(member1)
                .content("Last one, test from " + member1.getUser().getUsername())
                .chat(chat)
                .replyTo(null)
                .forwardedFrom(null)
                .createdAt(createdAt.plus(22, ChronoUnit.MINUTES))
                .build();

        MessageReceipt mr1 = MessageReceipt.builder()
                .id(null)
                .message(m1)
                .recipient(member2)
                .status(ReceiptStatus.READ)
                .deliveredAt(m1.getCreatedAt().plus(2, ChronoUnit.MINUTES))
                .readAt(m1.getCreatedAt().plus(3, ChronoUnit.MINUTES))
                .build();
        MessageReceipt mr2 = MessageReceipt.builder()
                .id(null)
                .message(m2)
                .recipient(member1)
                .status(ReceiptStatus.READ)
                .deliveredAt(m2.getCreatedAt().plus(2, ChronoUnit.MINUTES))
                .readAt(m2.getCreatedAt().plus(3, ChronoUnit.MINUTES))
                .build();
        MessageReceipt mr3 = MessageReceipt.builder()
                .id(null)
                .message(m3)
                .recipient(member1)
                .status(ReceiptStatus.READ)
                .deliveredAt(m3.getCreatedAt().plus(2, ChronoUnit.MINUTES))
                .readAt(m3.getCreatedAt().plus(3, ChronoUnit.MINUTES))
                .build();
        MessageReceipt mr4 = MessageReceipt.builder()
                .id(null)
                .message(m4)
                .recipient(member2)
                .status(ReceiptStatus.DELIVERED)
                .deliveredAt(m4.getCreatedAt().plus(2, ChronoUnit.MINUTES))
                .readAt(null)
                .build();

        chat.setMembers(List.of(member1, member2));
        chat.setMessages(List.of(m1, m2, m3, m4));

        saveChatWithCreatedAt(chat);
        messageReceiptRepository.saveAll(List.of(mr1, mr2, mr3, mr4));
    }

    private void initGroupChat(
            String name,
            String description,
            String imageUrl,
            Instant createdAt,
            List<User> users
    ) {
        Chat chat = Chat.builder()
                .id(null)
                .name(name)
                .description(description)
                .imageUrl(imageUrl)
                .members(new ArrayList<>())
                .messages(new ArrayList<>())
                .createdAt(createdAt)
                .type(ChatType.GROUP)
                .build();

        List<ChatMember> members = users.stream()
                .map(user -> ChatMember.builder()
                        .id(null)
                        .chat(chat)
                        .user(user)
                        .role(user == users.get(0) ? ChatRole.ADMIN : ChatRole.MEMBER)
                        .active(true)
                        .build())
                .toList();

        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ChatMember sender = members.get(i % members.size());
            messages.add(Message.builder()
                    .id(null)
                    .sender(sender)
                    .content(seedGroupMessage(name, i, sender.getUser().getFirstName()))
                    .chat(chat)
                    .replyTo(null)
                    .forwardedFrom(null)
                    .createdAt(createdAt.plus((i + 1L) * 8, ChronoUnit.HOURS))
                    .build());
        }

        List<MessageReceipt> receipts = new ArrayList<>();
        for (Message message : messages) {
            for (ChatMember member : members) {
                if (member == message.getSender()) {
                    continue;
                }

                receipts.add(MessageReceipt.builder()
                        .id(null)
                        .message(message)
                        .recipient(member)
                        .status(ReceiptStatus.READ)
                        .deliveredAt(message.getCreatedAt().plus(1, ChronoUnit.MINUTES))
                        .readAt(message.getCreatedAt().plus(4, ChronoUnit.MINUTES))
                        .build());
            }
        }

        chat.setMembers(members);
        chat.setMessages(messages);

        saveChatWithCreatedAt(chat);
        messageReceiptRepository.saveAll(receipts);
    }

    private void initUserActivities(Instant now, List<User> users) {
        List<UserActivity> activities = new ArrayList<>();

        for (int day = 0; day <= 27; day += 3) {
            for (int i = 0; i < users.size(); i++) {
                if ((day + i) % 2 == 0) {
                    activities.add(UserActivity.builder()
                            .id(null)
                            .user(users.get(i))
                            .type(UserActivityType.AUTHENTICATED_REQUEST)
                            .occurredAt(daysAgo(now, day).minus(i + 1L, ChronoUnit.HOURS))
                            .build());
                }
            }
        }

        for (User user : users) {
            activities.add(UserActivity.builder()
                    .id(null)
                    .user(user)
                    .type(UserActivityType.LOGIN)
                    .occurredAt(user.getLastActiveAt() == null ? hoursAgo(now, 12) : user.getLastActiveAt())
                    .build());
        }

        userActivityRepository.saveAll(activities);
    }

    private List<User> saveUsersWithCreatedAt(List<User> users) {
        return users.stream()
                .map(this::saveUserWithCreatedAt)
                .toList();
    }

    private User saveUserWithCreatedAt(User user) {
        Instant createdAt = user.getCreatedAt();
        User savedUser = userRepository.save(user);
        savedUser.setCreatedAt(createdAt);
        return userRepository.save(savedUser);
    }

    private void saveChatWithCreatedAt(Chat chat) {
        Instant createdAt = chat.getCreatedAt();
        List<Instant> messageCreatedAt = chat.getMessages().stream()
                .map(Message::getCreatedAt)
                .toList();

        Chat savedChat = chatRepository.save(chat);
        savedChat.setCreatedAt(createdAt);

        for (int i = 0; i < savedChat.getMessages().size(); i++) {
            savedChat.getMessages().get(i).setCreatedAt(messageCreatedAt.get(i));
        }

        chatRepository.save(savedChat);
    }

    private String seedGroupMessage(String chatName, int index, String firstName) {
        return switch (index) {
            case 0 -> firstName + " opened the " + chatName + " discussion.";
            case 1 -> firstName + " shared a short progress update.";
            case 2 -> firstName + " added notes for the next task.";
            case 3 -> firstName + " confirmed the current blockers.";
            case 4 -> firstName + " posted a follow-up question.";
            case 5 -> firstName + " attached the latest context.";
            case 6 -> firstName + " approved the proposed plan.";
            default -> firstName + " wrapped up the thread.";
        };
    }

    private Instant daysAgo(Instant now, long days) {
        return now.minus(days, ChronoUnit.DAYS);
    }

    private Instant hoursAgo(Instant now, long hours) {
        return now.minus(hours, ChronoUnit.HOURS);
    }
}
