package com.example.lumoo.domain.about;

import jakarta.persistence.*;

@Entity
@Table(name = "about_content")
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public String getStoryContent() { return storyContent; }
    public void setStoryContent(String storyContent) { this.storyContent = storyContent; }
    public String getMissionContent() { return missionContent; }
    public void setMissionContent(String missionContent) { this.missionContent = missionContent; }
    public String getVisionContent() { return visionContent; }
    public void setVisionContent(String visionContent) { this.visionContent = visionContent; }
    public String getFoundedYear() { return foundedYear; }
    public void setFoundedYear(String foundedYear) { this.foundedYear = foundedYear; }
    public String getTeamSize() { return teamSize; }
    public void setTeamSize(String teamSize) { this.teamSize = teamSize; }
    public String getProductsCount() { return productsCount; }
    public void setProductsCount(String productsCount) { this.productsCount = productsCount; }
}
