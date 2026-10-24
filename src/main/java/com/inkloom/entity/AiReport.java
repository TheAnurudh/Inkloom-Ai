package com.inkloom.entity;

import com.inkloom.enums.RiskLevel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Result of one AI analysis run. History is kept: every re-check creates a new row. */
@Entity
@Table(name = "ai_reports")
public class AiReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "content_item_id")
    private ContentItem contentItem;

    @Column(nullable = false) private int riskScore;                       // 0..100
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private RiskLevel riskLevel;
    @Column(nullable = false) private boolean meetsCriteria;
    @Column(nullable = false, length = 500) private String summary;
    @Column(name = "analysis_source", nullable = false, length = 20) private String analysisSource;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ai_report_flags", joinColumns = @JoinColumn(name = "report_id"))
    private List<RedFlag> flags = new ArrayList<>();

    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();

    protected AiReport() { }
    public AiReport(ContentItem item, int score, RiskLevel level, boolean meets, String summary, List<RedFlag> flags, String analysisSource) {
        this.analysisSource = analysisSource; this.contentItem = item; this.riskScore = score; this.riskLevel = level;
        this.meetsCriteria = meets; this.summary = summary; this.flags = new ArrayList<>(flags);
    }

    public Long getId() { return id; }
    public ContentItem getContentItem() { return contentItem; }
    public int getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public boolean isMeetsCriteria() { return meetsCriteria; }
    public String getSummary() { return summary; }
    public String getAnalysisSource() { return analysisSource; }
    public List<RedFlag> getFlags() { return flags; }
    public Instant getCreatedAt() { return createdAt; }
}
