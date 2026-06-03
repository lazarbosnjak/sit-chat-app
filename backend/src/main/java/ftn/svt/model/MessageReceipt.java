package ftn.svt.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
public class MessageReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Message message;

    @ManyToOne(optional = false)
    private ChatMember recipient;

    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;

    private Instant deliveredAt;

    private Instant readAt;
}
