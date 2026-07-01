package ftn.svt.service;

import ftn.svt.config.security.JwtUtils;
import ftn.svt.exception.ApiException;
import ftn.svt.model.PasswordResetToken;
import ftn.svt.model.RegistrationRequestForm;
import ftn.svt.model.RegistrationRequestFormStatus;
import ftn.svt.model.User;
import ftn.svt.model.UserActivityType;
import ftn.svt.model.dto.auth.LoginRequest;
import ftn.svt.model.dto.auth.PasswordResetConfirmRequest;
import ftn.svt.model.dto.auth.PasswordResetRequest;
import ftn.svt.model.dto.auth.RegistrationRequest;
import ftn.svt.repository.PasswordResetTokenRepository;
import ftn.svt.repository.RegistrationRequestFormRepository;
import ftn.svt.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofMinutes(5);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final RegistrationRequestFormRepository formRepository;
    private final UserActivityService userActivityService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

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

    @Transactional
    public Map<String, String> requestPasswordReset(@Valid PasswordResetRequest dto) {
        String identifier = dto.identifier().trim();

        userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .filter(User::isEnabled)
                .ifPresent(this::createAndSendPasswordResetLink);

        return Map.of(
                "message", "If an account exists, a password reset link has been sent.",
                "timestamp", Instant.now().toString()
        );
    }

    @Transactional
    public Map<String, String> resetPassword(@Valid PasswordResetConfirmRequest dto) {
        if (!dto.password().trim().equals(dto.repeatedPassword().trim())) {
            throw ApiException.badRequest("Passwords dont match");
        }

        Instant now = Instant.now();
        String tokenHash = hashToken(dto.token().trim());
        var resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired password reset link"));

        if (resetToken.getUsedAt() != null || !resetToken.getExpiresAt().isAfter(now)) {
            throw ApiException.badRequest("Invalid or expired password reset link");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.password()));
        resetToken.setUsedAt(now);

        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);

        return Map.of(
                "message", "Password has been reset successfully.",
                "timestamp", now.toString()
        );
    }

    private String resolveProfilePictureUrl(String pfpUrl) {
        if (pfpUrl == null || pfpUrl.isBlank()) {
            return User.DEFAULT_PROFILE_PICTURE_URL;
        }

        return pfpUrl.trim();
    }

    private void createAndSendPasswordResetLink(User user) {
        String token = generatePasswordResetToken();
        Instant expiresAt = Instant.now().plus(PASSWORD_RESET_TOKEN_TTL);

        passwordResetTokenRepository.deleteByUser_IdAndUsedAtIsNull(user.getId());
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .id(null)
                .tokenHash(hashToken(token))
                .user(user)
                .expiresAt(expiresAt)
                .usedAt(null)
                .createdAt(null)
                .build());

        String resetLink = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();

        emailService.sendPasswordResetLink(user, resetLink, expiresAt);
    }

    private String generatePasswordResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 hashing is unavailable", e);
        }
    }
}
