package com.example.lumoo.domain.shipping;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "riders")
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
               java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public int getTotalDeliveries() { return totalDeliveries; }
    public void setTotalDeliveries(int totalDeliveries) { this.totalDeliveries = totalDeliveries; }
}
