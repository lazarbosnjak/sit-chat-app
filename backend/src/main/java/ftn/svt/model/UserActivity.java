package ftn.svt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_activities",
        indexes = {
                @Index(name = "idx_user_activity_user_occurred_at", columnList = "user_id, occurred_at"),
                @Index(name = "idx_user_activity_occurred_at", columnList = "occurred_at")
        }
)
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserActivityType type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
