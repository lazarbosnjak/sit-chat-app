package ftn.svt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "users")
public class User {
    public static final String DEFAULT_PROFILE_PICTURE_URL =
            "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String email;

    private String pfpUrl;

    private boolean enabled;

    @Enumerated(EnumType.STRING)
    private UserBlockType blockType;

    private String blockReason;

    private Instant blockedAt;

    @CreatedDate
    private Instant createdAt;

    private Instant lastActiveAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
