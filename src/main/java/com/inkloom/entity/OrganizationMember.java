package com.inkloom.entity;

import com.inkloom.enums.OrgRole;
import jakarta.persistence.*;

/** "User X has role R in organization O" (one row per role). This table drives ALL authorization. */
@Entity
@Table(name = "organization_members",
       uniqueConstraints = @UniqueConstraint(name = "uk_member_org_user_role", columnNames = {"organization_id", "user_id", "role"}))
public class OrganizationMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private OrgRole role;

    @ManyToOne @JoinColumn(name = "team_id")
    private Team team;                              // optional

    protected OrganizationMember() { }
    public OrganizationMember(Organization organization, User user, OrgRole role, Team team) {
        this.organization = organization; this.user = user; this.role = role; this.team = team;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public User getUser() { return user; }
    public OrgRole getRole() { return role; }
    public Team getTeam() { return team; }
}
