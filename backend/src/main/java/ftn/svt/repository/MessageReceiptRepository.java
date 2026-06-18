package ftn.svt.repository;

import ftn.svt.model.MessageReceipt;
import ftn.svt.model.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, UUID> {
    List<MessageReceipt> findByMessageChatId(UUID chatId);

    @Query("""
            select r.message.chat.id, count(r)
            from MessageReceipt r
            where r.recipient.user.id = :userId
              and r.readAt is null
            group by r.message.chat.id
            """)
    List<Object[]> countUnreadByChatForUser(@Param("userId") UUID userId);

    @Query("""
            select count(r)
            from MessageReceipt r
            where r.message.chat.id = :chatId
              and r.recipient.user.id = :userId
              and r.readAt is null
            """)
    int countUnreadByChatIdAndUserId(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId
    );

    @Modifying
    @Query("""
            update MessageReceipt r
            set r.status = :status,
                r.readAt = :readAt
            where r.message.chat.id = :chatId
              and r.recipient.user.id = :userId
              and r.readAt is null
            """)
    int markChatAsRead(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId,
            @Param("status") ReceiptStatus status,
            @Param("readAt") Instant readAt
    );

}
