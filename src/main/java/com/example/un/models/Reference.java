package com.example.un.models;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reference")
public class Reference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String referenceId;

    private LocalDateTime processedAt;

    public Reference(String referenceId) {
        this.referenceId = referenceId;
        this.processedAt = LocalDateTime.now();
    }
}

