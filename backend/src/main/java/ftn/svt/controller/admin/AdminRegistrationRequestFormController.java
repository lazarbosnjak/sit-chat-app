package ftn.svt.controller.admin;

import ftn.svt.service.RegistrationRequestFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v0/admin/registration-requests")
@RequiredArgsConstructor
public class AdminRegistrationRequestFormController {

    private final RegistrationRequestFormService formService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @PageableDefault(sort = "status", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        return ResponseEntity.ok(formService.findAll(pageable));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id) {
        formService.approve(id);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id) {
        formService.reject(id);
        return ResponseEntity.noContent().build();
    }

}
