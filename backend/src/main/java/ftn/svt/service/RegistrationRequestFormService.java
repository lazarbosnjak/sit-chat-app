package ftn.svt.service;

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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationRequestFormService {

    private final RegistrationRequestFormRepository formRepository;
    private final UserRepository userRepository;

    public Page<RegistrationResponse> findAll(Pageable pageable) {
        return formRepository
                .findAll(pageable)
                .map(RegistrationResponse::from);
    }

    public void approve(UUID id) {
        RegistrationRequestForm form = formRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("registration request with this id not found"));
        if (!form.getStatus().equals(RegistrationRequestFormStatus.IN_PROCESS)) {
            return;
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

        userRepository.save(user);
    }

    public void reject(UUID id) {
        RegistrationRequestForm form = formRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("registration request with this id not found"));

        if (!form.getStatus().equals(RegistrationRequestFormStatus.IN_PROCESS)) {
            return;
        }
        form.setStatus(RegistrationRequestFormStatus.REJECTED);
        formRepository.save(form);
    }
}
