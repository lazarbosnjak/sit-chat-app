package ftn.svt.repository;

import ftn.svt.model.RegistrationRequestForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RegistrationRequestFormRepository extends JpaRepository<RegistrationRequestForm, UUID> {
}
