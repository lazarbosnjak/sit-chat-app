package ftn.svt.service;

import ftn.svt.config.security.JwtUtils;
import ftn.svt.exception.ApiException;
import ftn.svt.model.RegistrationRequestForm;
import ftn.svt.model.RegistrationRequestFormStatus;
import ftn.svt.model.User;
import ftn.svt.model.UserActivityType;
import ftn.svt.model.dto.auth.LoginRequest;
import ftn.svt.model.dto.auth.RegistrationRequest;
import ftn.svt.repository.RegistrationRequestFormRepository;
import ftn.svt.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final RegistrationRequestFormRepository formRepository;
    private final UserActivityService userActivityService;

    public Map<String, String> register(RegistrationRequest dto) {
        if (!dto.password().trim().equals(dto.repeatedPassword().trim())) {
            throw ApiException.badRequest("Passwords dont match");
        }
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw ApiException.conflict("Username already exists");
        }

        RegistrationRequestForm form = RegistrationRequestForm.builder()
                .requestId(null)
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .phoneNumber(dto.phoneNumber())
                .email(dto.email())
                .pfpUrl(resolveProfilePictureUrl(dto.pfpUrl()))
                .status(RegistrationRequestFormStatus.IN_PROCESS)
                .build();

        var savedForm = formRepository.save(form);

        String successMsg = "Your registration request has been sent to administrators for approval. You will be e-mailed the result.";
        Map<String, String> res = Map.of(
                "message", successMsg,
                "timestamp", Instant.now().toString(),
                "requestId", savedForm.getRequestId().toString()
        );

        return res;
    }

    public String login(@Valid LoginRequest dto) {
        User user = userRepository.findByUsername(dto.username())
                .orElseThrow(() -> ApiException.unauthorized("Invalid login credentials"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw ApiException.unauthorized("Invalid login credentials");
        }

        if (!user.isEnabled()) {
            String blockReason = user.getBlockReason() == null || user.getBlockReason().isBlank()
                    ? "No reason provided"
                    : user.getBlockReason();
            throw ApiException.forbidden("Your account is blocked. Reason: " + blockReason);
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(dto.username());
            String token = jwtUtils.generateToken(userDetails, user.getId());
            userActivityService.recordActivity(user.getId(), UserActivityType.LOGIN);
            return token;
        } catch (UsernameNotFoundException ex) {
            throw ApiException.unauthorized(ex.getMessage());
        }
    }

    private String resolveProfilePictureUrl(String pfpUrl) {
        if (pfpUrl == null || pfpUrl.isBlank()) {
            return User.DEFAULT_PROFILE_PICTURE_URL;
        }

        return pfpUrl.trim();
    }
}
