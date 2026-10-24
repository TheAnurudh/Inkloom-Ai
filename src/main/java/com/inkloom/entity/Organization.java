package com.inkloom.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** A company / startup / agency workspace. */
@Entity
@Table(name = "organizations", uniqueConstraints = @UniqueConstraint(name = "uk_org_name", columnNames = "name"))
public class Organization {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(optional = false) @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Organization() { }

    public Organization(String name, String description, User createdBy) {
        this.name = name; this.description = description; this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
