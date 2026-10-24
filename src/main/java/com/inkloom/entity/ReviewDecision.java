package com.inkloom.entity;

import com.inkloom.enums.Decision;
import jakarta.persistence.*;
import java.time.Instant;

/** The final answer a reviewer gave for a task (immutable record). */
@Entity
@Table(name = "review_decisions")
public class ReviewDecision {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false) @JoinColumn(name = "task_id", unique = true)
    private ReviewTask task;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Decision decision;

    @Column(name = "review_comment", length = 1000) private String comment;

    @ManyToOne(optional = false) @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Column(nullable = false, updatable = false) private Instant decidedAt = Instant.now();

    protected ReviewDecision() { }
    public ReviewDecision(ReviewTask task, Decision decision, String comment, User decidedBy) {
        this.task = task; this.decision = decision; this.comment = comment; this.decidedBy = decidedBy;
    }

    public Long getId() { return id; }
    public ReviewTask getTask() { return task; }
    public Decision getDecision() { return decision; }
    public String getComment() { return comment; }
    public User getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
}
