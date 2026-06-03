package ftn.svt.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Chat chat;

    @ManyToOne
    private User user;

    @Enumerated(EnumType.STRING)
    private ChatRole role;
}
