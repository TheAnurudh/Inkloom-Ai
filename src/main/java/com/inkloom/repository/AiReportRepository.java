package com.inkloom.repository;

import com.inkloom.entity.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {
    Optional<AiReport> findFirstByContentItemIdOrderByCreatedAtDesc(Long contentItemId);
}
