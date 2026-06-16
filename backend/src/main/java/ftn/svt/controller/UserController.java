package ftn.svt.controller;

import ftn.svt.exception.ApiException;
import ftn.svt.model.User;
import ftn.svt.model.dto.user.UserInfoDTO;
import ftn.svt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v0/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllFiltered(
            @RequestParam() String search,
            Pageable pageable
    ) {
        if (search.isEmpty()) {
            throw ApiException.badRequest("search must be at least 1 character long");
        }
        Page<User> users = userService.getAllFiltered(search, pageable);
        Page<UserInfoDTO> dtos = users.map(UserInfoDTO::from);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication auth) {
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
