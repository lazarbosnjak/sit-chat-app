package ftn.svt.repository;

import ftn.svt.model.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    Optional<MessageReaction> findByMessageIdAndReactorId(UUID messageId, UUID reactorId);

    List<MessageReaction> findByMessageId(UUID messageId);

    List<MessageReaction> findByMessageIdIn(Collection<UUID> messageIds);
}
