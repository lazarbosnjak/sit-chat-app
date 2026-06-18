package ftn.svt.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id", "recipient_id"})
    }
)
public class MessageReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @EqualsAndHashCode.Include
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Message message;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ChatMember recipient;

    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;

    private Instant deliveredAt;

    private Instant readAt;
}
