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
                @UniqueConstraint(columnNames = {"message_id", "user_id"})
        }
)
public class StarredMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Message message;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @CreatedDate
    private Instant createdAt;
}
