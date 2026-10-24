package com.inkloom.entity;

import jakarta.persistence.*;

/** Team inside an organization (Marketing, Design, Product, Legal...). */
@Entity
@Table(name = "teams", uniqueConstraints = @UniqueConstraint(name = "uk_team_org_name", columnNames = {"organization_id", "name"}))
public class Team {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 80)
    private String name;

    protected Team() { }
    public Team(Organization organization, String name) { this.organization = organization; this.name = name; }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getName() { return name; }
}
