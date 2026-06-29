package ftn.svt.repository;

import ftn.svt.model.Chat;
import ftn.svt.model.ChatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {
    long countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            ChatType type,
            Instant rangeStart,
            Instant rangeEnd
    );

    @Query(
            value = """
                    select date_trunc(:bucket, c.created_at at time zone 'UTC'), count(*)
                    from chat c
                    where c.type = :chatType
                        and c.created_at >= :rangeStart
                        and c.created_at < :rangeEnd
                    group by 1
                    order by 1
                    """,
            nativeQuery = true
    )
    List<Object[]> countCreatedChatsByTypeByBucket(
            @Param("chatType") String chatType,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("bucket") String bucket
    );

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

    @Query("""
            select c
            from Chat c
            join c.members m
            where m.user.id = :id
            """)
    Collection<Chat> findAllWithUserId(UUID id);

    boolean existsByIdAndMembersUserUsername(UUID chatId, String username);
}
