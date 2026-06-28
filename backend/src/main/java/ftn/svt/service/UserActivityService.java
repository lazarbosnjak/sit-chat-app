package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.User;
import ftn.svt.model.UserActivity;
import ftn.svt.model.UserActivityType;
import ftn.svt.repository.UserActivityRepository;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;

    @Transactional
    public void recordActivity(UUID userId, UserActivityType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        Instant now = Instant.now();
        user.setLastActiveAt(now);

        UserActivity activity = UserActivity.builder()
                .user(user)
                .type(type)
                .occurredAt(now)
                .build();

        userActivityRepository.save(activity);
    }
}
