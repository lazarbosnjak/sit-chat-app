package ftn.svt.repository;

import ftn.svt.model.MessageReceipt;
import ftn.svt.model.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, UUID> {
    List<MessageReceipt> findByMessageChatId(UUID chatId);

    List<MessageReceipt> findByMessageId(UUID messageId);

    List<MessageReceipt> findByMessageIdIn(Collection<UUID> messageIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MessageReceipt r
            set r.status = :deliveredStatus,
                r.deliveredAt = :deliveredAt
            where r.message.id = :messageId
              and r.status = :sentStatus
            """)
    int markMessageAsDelivered(
            @Param("messageId") UUID messageId,
            @Param("sentStatus") ReceiptStatus sentStatus,
            @Param("deliveredStatus") ReceiptStatus deliveredStatus,
            @Param("deliveredAt") Instant deliveredAt
    );

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

    @Query("""
            select r
            from MessageReceipt r
            where r.message.chat.id = :chatId
              and r.recipient.user.id = :userId
              and r.readAt is null
            """)
    List<MessageReceipt> findUnreadByChatIdAndUserId(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId
    );

}
