package com.example.lumoo.domain.shipping;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "riders")
@Getter
@Setter
public class Rider {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType = VehicleType.MOTORCYCLE;

    private String area;
    private boolean active = true;
    private LocalDateTime joinedAt;
    private int totalDeliveries = 0;

    @PrePersist
    protected void onCreate() { this.joinedAt = LocalDateTime.now(); }

    public enum VehicleType { MOTORCYCLE, VAN, TRUCK, BICYCLE }

    public String whatsappLink(String message) {
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (!cleaned.startsWith("+")) cleaned = "+220" + cleaned.replaceAll("^0", "");
        return "https://wa.me/" + cleaned.replace("+", "") + "?text=" +
               URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

}
