package ftn.svt.repository;

import ftn.svt.model.ChatMember;
import ftn.svt.model.ChatRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMemberRepository extends JpaRepository<ChatMember, UUID> {

    Optional<ChatMember> findByChatIdAndUserId(UUID chatId, UUID userId);

    Optional<ChatMember> findByIdAndChatIdAndActiveTrue(UUID memberId, UUID chatId);

    Optional<ChatMember> findByChatIdAndUserUsernameAndActiveTrue(UUID chatId, String username);

    boolean existsByChatIdAndUserUsernameAndRoleAndActiveTrue(
            UUID chatId,
            String username,
            ChatRole role
    );

    List<ChatMember> findAllByChatIdAndActiveTrue(UUID chatId);

    long countByChatIdAndRoleAndActiveTrue(UUID chatId, ChatRole role);

    @Query("""
            select cm.user.username
            from ChatMember cm
            where cm.chat.id = :chatId
              and cm.active = true
            """)
    Collection<String> findUsernamesByChatId(@Param("chatId") UUID chatId);
}
