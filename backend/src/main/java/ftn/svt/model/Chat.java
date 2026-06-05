package ftn.svt.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@EntityListeners(AuditingEntityListener.class)
@Entity
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String imageUrl;

    @ManyToMany
    Set<ChatMember> members;

    @CreatedDate
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private ChatType type;

}
