package com.example.lumoo.domain.about;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "team_members")
@Getter
@Setter
public class TeamMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String role;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String imageUrl;
    private int displayOrder = 0;
    private boolean active = true;

}
