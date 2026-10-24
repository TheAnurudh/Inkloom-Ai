package com.inkloom.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** A product published by a company, with the facts creators ARE allowed / NOT allowed to claim. */
@Entity
@Table(name = "products")
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 120) private String brand;
    @Column(length = 2000) private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_approved_claims", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "claim", length = 300)
    private Set<String> approvedClaims = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_forbidden_claims", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "claim", length = 300)
    private Set<String> forbiddenClaims = new LinkedHashSet<>();

    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();

    protected Product() { }
    public Product(Organization organization, String name, String brand, String description,
                   Set<String> approved, Set<String> forbidden) {
        this.organization = organization; this.name = name; this.brand = brand; this.description = description;
        this.approvedClaims = approved; this.forbiddenClaims = forbidden;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getDescription() { return description; }
    public Set<String> getApprovedClaims() { return approvedClaims; }
    public Set<String> getForbiddenClaims() { return forbiddenClaims; }
    public boolean isActive() { return active; }
}
