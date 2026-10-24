package com.inkloom.entity;

import com.inkloom.enums.OrgRole;
import com.inkloom.enums.TaskStatus;
import jakarta.persistence.*;
import java.time.Instant;

/** "Reviewer X must review content C at step N" (one pending task at a time = sequential approval). */
@Entity
@Table(name = "review_tasks",
       uniqueConstraints = @UniqueConstraint(name = "uk_task_content_round_step", columnNames = {"content_item_id", "review_round", "step"}))
public class ReviewTask {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "content_item_id")
    private ContentItem contentItem;

    @Column(name = "review_round", nullable = false) private int reviewRound;
    @Column(nullable = false) private int step;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private OrgRole requiredRole;

    @ManyToOne(optional = false) @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    private Instant completedAt;

    @Version private Long version;                  // two simultaneous decisions on one task -> second one fails

    protected ReviewTask() { }
    public ReviewTask(ContentItem item, int round, int step, OrgRole role, User reviewer) {
        this.contentItem = item; this.reviewRound = round; this.step = step;
        this.requiredRole = role; this.reviewer = reviewer;
    }

    public void complete() { this.status = TaskStatus.COMPLETED; this.completedAt = Instant.now(); }

    public Long getId() { return id; }
    public ContentItem getContentItem() { return contentItem; }
    public int getReviewRound() { return reviewRound; }
    public int getStep() { return step; }
    public OrgRole getRequiredRole() { return requiredRole; }
    public User getReviewer() { return reviewer; }
    public TaskStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
