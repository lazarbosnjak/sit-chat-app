package ftn.svt.repository;

import ftn.svt.model.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMemberRepository extends JpaRepository<ChatMember, UUID> {

    Optional<ChatMember> findByChatIdAndUserUsername(UUID chatId, String username);

    List<ChatMember> findAllByChatId(UUID chatId);

    @Query("""
            select cm.user.username
            from ChatMember cm
            where cm.chat.id = :chatId
            """)
    Collection<String> findUsernamesByChatId(@Param("chatId") UUID chatId);
}
