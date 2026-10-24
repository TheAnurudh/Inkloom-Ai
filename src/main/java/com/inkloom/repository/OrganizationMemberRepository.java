package com.inkloom.repository;

import com.inkloom.entity.OrganizationMember;
import com.inkloom.enums.OrgRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    List<OrganizationMember> findByOrganizationId(Long organizationId);
    List<OrganizationMember> findByUserId(Long userId);
    List<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    List<OrganizationMember> findByOrganizationIdAndRole(Long organizationId, OrgRole role);
    boolean existsByOrganizationIdAndUserIdAndRole(Long organizationId, Long userId, OrgRole role);
}
