package com.example.lumoo.domain.about;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "company_values")
@Getter
@Setter
public class CompanyValue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String icon;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int displayOrder = 0;

}
