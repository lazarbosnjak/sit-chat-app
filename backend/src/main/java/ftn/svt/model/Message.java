package ftn.svt.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(optional = false)
    private ChatMember sender;

    @Column(nullable = false)
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "message_type")
    private MessageType type = MessageType.TEXT;

    private UUID audioId;

    private Integer audioDurationMs;

    private String audioContentType;

    private Long audioSizeBytes;

    @Builder.Default
    private boolean systemMessage = false;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Chat chat;

    @ManyToOne
    private Message replyTo;

    @ManyToOne
    private Message forwardedFrom;

    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageReaction> reactions = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

}
