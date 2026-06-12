package ftn.svt.service;

import ftn.svt.config.security.JwtUtils;
import ftn.svt.model.RegistrationRequestForm;
import ftn.svt.model.RegistrationRequestFormStatus;
import ftn.svt.model.dto.auth.LoginRequest;
import ftn.svt.model.dto.auth.RegistrationRequest;
import ftn.svt.repository.RegistrationRequestFormRepository;
import ftn.svt.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RegistrationRequestFormRepository formRepository;

    public Map<String, String> register(RegistrationRequest dto) {
        if (!dto.password().trim().equals(dto.repeatedPassword().trim())) {
            // TODO: return bad request from this
            throw new RuntimeException("Passwords dont match");
        }
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            // TODO: return bad request from this
            throw new RuntimeException("Username already exists");
        }
        RegistrationRequestForm form = RegistrationRequestForm.builder()
                .requestId(null)
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .phoneNumber(dto.phoneNumber())
                .email(dto.email())
                .pfpUrl(dto.pfpUrl())
                .status(RegistrationRequestFormStatus.IN_PROCESS)
                .build();

        formRepository.save(form);
        // TODO: Error check if this fails

        String successMsg =  "Your registration request has been sent to administrators for approval. You will be e-mailed the result.";
        Map<String, String> res = Map.of(
                "message", successMsg,
                "timestamp", Instant.now().toString()
        );

        return res;
    }

    public String login(@Valid LoginRequest dto) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(dto.username(), dto.password());
        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.username());
        return jwtUtils.generateToken(userDetails);
    }
}
