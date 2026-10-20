package com.example.lumoo.domain.user;
import lombok.Getter;
import lombok.Setter;
import com.example.lumoo.infrastructure.security.EncryptedStringConverter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
@Entity @Table(name = "users")
@Getter
@Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    @JsonIgnore // never in Redis cache / JSON output
    private String password;
    @Column(unique = true)
    private String email;
    private String fullName;
    @Convert(converter = EncryptedStringConverter.class)
    private String phone;
    @Convert(converter = EncryptedStringConverter.class)
    private String address;
    private String profilePicture;
    @JsonIgnore // never in Redis cache / JSON output
    private String resetToken;
    @JsonIgnore // never in Redis cache / JSON output
    private LocalDateTime tokenExpiry;
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean verified;
    private boolean suspended;
    private boolean emailVerified;
    @JsonIgnore // never in Redis cache / JSON output
    private String emailVerificationToken;
    /** True for accounts registered after login started requiring a verified email. */
    @Column(name = "email_verification_required", nullable = false)
    private boolean emailVerificationRequired;
    @JsonIgnore // never in Redis cache / JSON output
    private String payoutPhone;
    private String payoutNetwork;
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private User vendor;
    public String getDisplayName() {
        if (fullName != null && !fullName.isBlank()) return fullName;
        if (username != null && !username.isBlank())
            return username.contains("@") ? username.split("@")[0] : username;
        return email != null ? email.split("@")[0] : "User";
    }
}
