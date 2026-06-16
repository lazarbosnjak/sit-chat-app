package ftn.svt.repository;

import ftn.svt.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {
        @Query("""
                    select c
                    from Chat c
                    join c.members m
                    where m.user.id in :memberIds
                    group by c.id
                    having count(distinct m.user.id) = :memberCount
                       and (
                           select count(cm)
                           from ChatMember cm
                           where cm.chat = c
                       ) = :memberCount
                """)
        Optional<Chat> findByExactMemberIds(
            @Param("memberIds") Set<UUID> memberIds,
            @Param("memberCount") long memberCount
    );
}
