package ftn.svt.controller;

import ftn.svt.model.User;
import ftn.svt.model.dto.auth.LoginRequest;
import ftn.svt.model.dto.auth.RegistrationRequest;
import ftn.svt.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v0/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationRequest dto) {
        var registrationResponse = authService.register(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(registrationResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginRequest dto) {
        try {
            String token = authService.login(dto);
            return ResponseEntity.ok(token);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
