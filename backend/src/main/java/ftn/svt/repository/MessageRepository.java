package ftn.svt.repository;

import ftn.svt.model.ChatType;
import ftn.svt.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findAllByChatId(UUID chatId);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Instant rangeStart,
            Instant rangeEnd
    );

    @Query(
            value = """
                    select date_trunc(:bucket, m.created_at at time zone 'UTC'), count(*)
                    from message m
                    where m.created_at >= :rangeStart and m.created_at < :rangeEnd
                    group by 1
                    order by 1
                    """,
            nativeQuery = true
    )
    List<Object[]> countMessagesByBucket(
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("bucket") String bucket
    );

    @Query("""
            select u.id, u.username, u.firstName, u.lastName, u.pfpUrl, count(m)
            from Message m
            join m.sender sender
            join sender.user u
            where m.createdAt >= :rangeStart and m.createdAt < :rangeEnd
            group by u.id, u.username, u.firstName, u.lastName, u.pfpUrl
            order by count(m) desc, u.username
            """)
    List<Object[]> findTopMessageSenders(
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            Pageable pageable
    );

    @Query("""
            select chat.id, chat.name, chat.imageUrl, count(m)
            from Message m
            join m.chat chat
            where chat.type = :chatType
                and m.createdAt >= :rangeStart
                and m.createdAt < :rangeEnd
            group by chat.id, chat.name, chat.imageUrl
            order by count(m) desc, coalesce(chat.name, '')
            """)
    List<Object[]> findTopChatsByMessageCount(
            @Param("chatType") ChatType chatType,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            Pageable pageable
    );
}
