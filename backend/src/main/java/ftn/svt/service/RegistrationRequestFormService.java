package ftn.svt.service;

import ftn.svt.model.dto.auth.RegistrationResponse;
import ftn.svt.repository.RegistrationRequestFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationRequestFormService {

    private final RegistrationRequestFormRepository formRepository;

    public Page<RegistrationResponse> findAll(Pageable pageable) {
        return formRepository
                .findAll(pageable)
                .map(RegistrationResponse::from);
    }
}
