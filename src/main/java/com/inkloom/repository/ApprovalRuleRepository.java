package com.inkloom.repository;

import com.inkloom.entity.ApprovalRule;
import com.inkloom.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, Long> {
    List<ApprovalRule> findByOrganizationId(Long organizationId);
    Optional<ApprovalRule> findByOrganizationIdAndContentType(Long organizationId, ContentType contentType);
}
