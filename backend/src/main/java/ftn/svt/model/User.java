package ftn.svt.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "users")
public class User {

    @Id
    private UUID id;

    private String username;

    private String password;

    private UserRole role;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String email;

    private String pfpUrl;

    private boolean enabled = true;

}
