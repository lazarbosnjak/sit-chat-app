package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.User;
import ftn.svt.model.dto.user.UpdateUserProfileRequest;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with username: " + username + " not found"));
    }

    public User findOneById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    public Collection<User> getAllFiltered(String search, Principal principal) {
        UUID principalUserId = findByUsername(principal.getName()).getId();
        return userRepository.findAllFiltered(search, principalUserId);
    }

    @Transactional
    public User updateProfile(UUID id, UpdateUserProfileRequest dto) {
        User user = findOneById(id);

        byte changes = 0;

        if (!user.getFirstName().equals(dto.firstName())) {
            user.setFirstName(dto.firstName());
            changes++;
        }
        if (!user.getLastName().equals(dto.lastName())) {
            user.setLastName(dto.lastName());
            changes++;
        }
        if (!user.getPhoneNumber().equals(dto.phoneNumber())) {
            user.setPhoneNumber(dto.phoneNumber());
            changes++;
        }
        if (!user.getEmail().equals(dto.email())) {
            user.setEmail(dto.email());
            changes++;
        }
        if (!user.getPfpUrl().equals(dto.pfpUrl())) {
            user.setPfpUrl(dto.pfpUrl());
            changes++;
        }

        if (changes == 0) {
           return user;
        }

        return userRepository.save(user);
    }
}
