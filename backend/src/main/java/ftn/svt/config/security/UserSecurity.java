package ftn.svt.config.security;

import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSecurity {

    private final String ROLE_ADMIN = "ROLE_ADMIN";
    private final UserRepository userRepository;

    public boolean isSelfOrAdmin(Authentication authentication, UUID userId) {
        if (authentication == null || !authentication.isAuthenticated() || userId == null) {
            return false;
        }

        if (hasAdminRole(authentication)) {
            return true;
        }

        return userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId().equals(userId))
                .orElse(false);
     }

     private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
     }
}
