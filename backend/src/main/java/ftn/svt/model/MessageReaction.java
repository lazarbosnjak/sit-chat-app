package ftn.svt.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"message_id", "reactor_id"})
        }
)
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Message message;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ChatMember reactor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageReactionType type;

    @CreatedDate
    private Instant createdAt;
}
