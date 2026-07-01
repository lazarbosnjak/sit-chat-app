package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.RegistrationRequestForm;
import ftn.svt.model.RegistrationRequestFormStatus;
import ftn.svt.model.User;
import ftn.svt.model.UserRole;
import ftn.svt.model.dto.auth.RegistrationResponse;
import ftn.svt.repository.RegistrationRequestFormRepository;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationRequestFormService {

    private final RegistrationRequestFormRepository formRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public Page<RegistrationResponse> findAll(Pageable pageable) {
        return formRepository
                .findAll(pageable)
                .map(RegistrationResponse::from);
    }

    @Transactional
    public void approve(UUID id) {
        RegistrationRequestForm form = formRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("registration request with this id not found"));
        if (form.getStatus() != RegistrationRequestFormStatus.IN_PROCESS) {
            throw ApiException.conflict("Registration request has already been processed");
        }

        form.setStatus(RegistrationRequestFormStatus.APPROVED);
        formRepository.save(form);

        User user = User.builder()
                .id(null)
                .username(form.getUsername())
                .password(form.getPassword())
                .role(UserRole.USER)
                .firstName(form.getFirstName())
                .lastName(form.getLastName())
                .phoneNumber(form.getPhoneNumber())
                .email(form.getEmail())
                .pfpUrl(form.getPfpUrl())
                .enabled(true) // TODO: Maybe add e-mail enabled later?
                .createdAt(null)
                .build();

        User savedUser = userRepository.save(user);
        emailService.sendRegistrationApprovedEmail(savedUser);
    }

    @Transactional
    public void reject(UUID id) {
        RegistrationRequestForm form = formRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("registration request with this id not found"));

        if (form.getStatus() != RegistrationRequestFormStatus.IN_PROCESS) {
            throw ApiException.conflict("Registration request has already been processed");
        }
        form.setStatus(RegistrationRequestFormStatus.REJECTED);
        formRepository.save(form);
    }
}
