package ftn.svt.service;

import ftn.svt.config.security.JwtUtils;
import ftn.svt.model.User;
import ftn.svt.model.UserRole;
import ftn.svt.model.dto.auth.LoginRequest;
import ftn.svt.model.dto.auth.RegistrationRequest;
import ftn.svt.model.dto.auth.RegistrationResponse;
import ftn.svt.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public RegistrationResponse register(RegistrationRequest dto) {
        if (!dto.password().trim().equals(dto.repeatedPassword().trim())) {
            // TODO: return bad request from this
            throw new RuntimeException("Passwords dont match");
        }
        User user = User.builder()
                .id(null)
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .role(UserRole.USER)
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .phoneNumber(dto.phoneNumber())
                .email(dto.email())
                .pfpUrl(dto.pfpUrl())
                // TODO: Change to false when registration form for admin gets added
                .enabled(true)
                .build();

        // TODO: Check if user exists before saving
        User savedUser = userRepository.save(user);

        RegistrationResponse resp = new RegistrationResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getPhoneNumber(),
                savedUser.getEmail(),
                savedUser.getPfpUrl()
        );

        return resp;
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
