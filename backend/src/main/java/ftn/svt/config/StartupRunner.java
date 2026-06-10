package ftn.svt.config;

import ftn.svt.model.User;
import ftn.svt.model.UserRole;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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

        User user = User.builder()
        .id(null)
        .username("admin")
        .password(passwordEncoder.encode("admin123"))
        .role(UserRole.ADMIN)
        .firstName("Admin")
        .lastName("Adminovic")
        .phoneNumber("+381600000000")
        .email("admin@admin.com")
        .pfpUrl(null)
        .enabled(true)
        .createdAt(null)
        .build();

        User savedUser = userRepository.save(user);
        System.out.println(savedUser);
    }
}
