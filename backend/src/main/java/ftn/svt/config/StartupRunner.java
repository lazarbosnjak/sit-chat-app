package ftn.svt.config;

import ftn.svt.model.User;
import ftn.svt.model.UserRole;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                .pfpUrl( "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg")
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
                .pfpUrl( "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg")
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
                .pfpUrl( "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg")
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
                .pfpUrl( "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg")
                .enabled(false)
                .createdAt(null)
                .build();

        userRepository.saveAll(
                List.of(user1, user2, user3, user4)
        );
    }
}
