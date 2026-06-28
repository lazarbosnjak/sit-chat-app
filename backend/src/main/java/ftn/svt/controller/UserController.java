package ftn.svt.controller;

import ftn.svt.model.User;
import ftn.svt.model.dto.user.UpdateUserProfileRequest;
import ftn.svt.model.dto.user.UserInfoDTO;
import ftn.svt.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
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
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) Boolean hasProfilePicture,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastActiveFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastActiveTo,
            Principal principal
    ) {
        boolean hasCriteria = !search.isBlank()
                || hasProfilePicture != null
                || lastActiveFrom != null
                || lastActiveTo != null;

        if (!hasCriteria) {
            return ResponseEntity.ok(List.of());
        }

        Collection<User> users = userService.getAllFiltered(
                search,
                hasProfilePicture,
                lastActiveFrom,
                lastActiveTo,
                principal
        );
        List<UserInfoDTO> dtos = users.stream().map(UserInfoDTO::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        return ResponseEntity.ok(UserInfoDTO.from(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOneById(@PathVariable UUID id) {
        User user = userService.findOneById(id);
        return ResponseEntity.ok(UserInfoDTO.from(user));
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
