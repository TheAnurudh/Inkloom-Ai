package com.inkloom.repository;

import com.inkloom.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);
}
