package com.example.lumoo.domain.about;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "about_content")
@Getter
@Setter
public class AboutContent {

    @Id
    private Long id = 1L;

    private String heroTitle;

    @Column(columnDefinition = "TEXT")
    private String heroSubtitle;

    @Column(columnDefinition = "TEXT")
    private String storyContent;

    @Column(columnDefinition = "TEXT")
    private String missionContent;

    @Column(columnDefinition = "TEXT")
    private String visionContent;

    private String foundedYear;
    private String teamSize;
    private String productsCount;

}
