package ftn.svt.controller;

import ftn.svt.model.User;
import ftn.svt.model.dto.user.UserInfoDTO;
import ftn.svt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v0/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        User user = userService.findByUsername(auth.getName());
        UserInfoDTO dto = new UserInfoDTO(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getPfpUrl(),
                user.getRole().toString(),
                user.getCreatedAt(),
                user.isEnabled()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOneById(@PathVariable UUID id) {
        User user = userService.findOneById(id);
        UserInfoDTO dto = new UserInfoDTO(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getPfpUrl(),
                user.getRole().toString(),
                user.getCreatedAt(),
                user.isEnabled()
        );
        return ResponseEntity.ok(dto);
    }
}
