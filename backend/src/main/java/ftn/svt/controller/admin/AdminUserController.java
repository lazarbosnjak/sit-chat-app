package ftn.svt.controller.admin;

import ftn.svt.model.User;
import ftn.svt.model.dto.user.UpdateUserRequest;
import ftn.svt.model.dto.user.UserInfoDTO;
import ftn.svt.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll(
    ) {
        Collection<User> users = userService.getAll();
        List<UserInfoDTO> dtos = users.stream().map(UserInfoDTO::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateById(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateUserRequest dto
    ) {

        User updatedUser = userService.update(id, dto);

        return ResponseEntity
                .ok(UserInfoDTO.from(updatedUser));
    }
}
