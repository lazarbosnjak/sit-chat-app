package ftn.svt.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Data
@EntityListeners(AuditingEntityListener.class)
@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private ChatMember sender;

    @Column(nullable = false)
    private String content;

    @ManyToOne(optional = false)
    private Chat chat;

    @ManyToOne
    private Message replyTo;

    @ManyToOne
    private Message forwardedFrom;

    @CreatedDate
    private Instant createdAt;

    // TODO: Add reactions

}
