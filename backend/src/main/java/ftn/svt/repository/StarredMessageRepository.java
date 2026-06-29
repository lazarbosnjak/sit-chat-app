package ftn.svt.repository;

import ftn.svt.model.StarredMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StarredMessageRepository extends JpaRepository<StarredMessage, UUID> {
    Optional<StarredMessage> findByMessageIdAndUserId(UUID messageId, UUID userId);

    List<StarredMessage> findAllByUserUsernameOrderByCreatedAtDesc(String username);

    List<StarredMessage> findByMessageIdInAndUserUsername(Collection<UUID> messageIds, String username);
}
