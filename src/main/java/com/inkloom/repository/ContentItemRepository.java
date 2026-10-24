package com.inkloom.repository;

import com.inkloom.entity.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {
    List<ContentItem> findBySubmittedByIdOrderByCreatedAtDesc(Long userId);

    @Query("select c from ContentItem c where c.product.organization.id = :orgId order by c.createdAt desc")
    List<ContentItem> findByOrganization(@Param("orgId") Long orgId);
}
