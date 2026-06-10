package ftn.svt.controller.admin;

import ftn.svt.service.RegistrationRequestFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v0/admin/registration-requests")
@RequiredArgsConstructor
public class AdminRegistrationRequestFormController {

    private final RegistrationRequestFormService formService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        return ResponseEntity.ok(formService.findAll(pageable));
    }

}
