package ftn.svt.controller.admin;

import ftn.svt.model.User;
import ftn.svt.model.dto.user.UserInfoDTO;
import ftn.svt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

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
}
