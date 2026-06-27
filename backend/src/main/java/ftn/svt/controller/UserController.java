package ftn.svt.controller;

import ftn.svt.exception.ApiException;
import ftn.svt.model.User;
import ftn.svt.model.dto.user.UpdateUserProfileRequest;
import ftn.svt.model.dto.user.UserInfoDTO;
import ftn.svt.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllFiltered(
            @RequestParam() String search,
            Principal principal
    ) {
        if (search.isBlank()) {
            throw ApiException.badRequest("search must be at least 1 character long");
        }

        Collection<User> users = userService.getAllFiltered(search, principal);
        List<UserInfoDTO> dtos = users.stream().map(UserInfoDTO::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        UserInfoDTO dto = new UserInfoDTO(
                user.getId(),
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
                user.getId(),
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

    @PreAuthorize("@userSecurity.isSelf(authentication, #id)")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateById(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateUserProfileRequest dto
    ) {

        User updatedUser = userService.updateProfile(id, dto);

        return ResponseEntity
                .ok(UserInfoDTO.from(updatedUser));
    }
}
