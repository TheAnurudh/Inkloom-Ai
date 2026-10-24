package com.inkloom.repository;

import com.inkloom.entity.ReviewTask;
import com.inkloom.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {
    List<ReviewTask> findByReviewerIdAndStatusOrderByCreatedAtAsc(Long reviewerId, TaskStatus status);
    List<ReviewTask> findByContentItemIdOrderByStepAsc(Long contentItemId);
    long countByReviewerIdAndStatus(Long reviewerId, TaskStatus status);
}
