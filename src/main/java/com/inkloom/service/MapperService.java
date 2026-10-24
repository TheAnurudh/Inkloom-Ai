package com.inkloom.service;

import com.inkloom.dto.Dtos.*;
import com.inkloom.entity.*;
import com.inkloom.repository.AiReportRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Entity -> DTO conversion. We never return entities directly (would leak password hashes, internals). */
@Component
public class MapperService {

    private final AiReportRepository reportRepo;

    public MapperService(AiReportRepository reportRepo) { this.reportRepo = reportRepo; }

    public UserResponse toUser(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getAccountType());
    }

    public ProductResponse toProduct(Product p) {
        return new ProductResponse(p.getId(), p.getOrganization().getId(), p.getOrganization().getName(), p.getName(),
                p.getBrand(), p.getDescription(), new ArrayList<>(p.getApprovedClaims()), new ArrayList<>(p.getForbiddenClaims()));
    }

    private List<RedFlagResponse> flags(List<RedFlag> in) {
        List<RedFlagResponse> out = new ArrayList<>();
        for (RedFlag f : in) out.add(new RedFlagResponse(f.getIssue(), f.getReason(), f.getSuggestedAction(), f.getSeverity()));
        return out;
    }

    /** For "instant check" (nothing saved). */
    public AiReportResponse toReportFromResult(AnalysisResult r, Instant at) {
        return new AiReportResponse(r.score(), r.level(), r.meetsCriteria(), r.summary(), flags(r.flags()), at, r.source());
    }

    public AiReportResponse toReport(AiReport r) {
        return new AiReportResponse(r.getRiskScore(), r.getRiskLevel(), r.isMeetsCriteria(), r.getSummary(),
                flags(r.getFlags()), r.getCreatedAt(), r.getAnalysisSource());
    }

    public ContentResponse toContent(ContentItem c) {
        AiReportResponse report = reportRepo.findFirstByContentItemIdOrderByCreatedAtDesc(c.getId())
                .map(r -> toReport(r)).orElse(null);
        Product p = c.getProduct();
        return new ContentResponse(c.getId(), c.getTitle(), c.getContentType(), c.getStatus(), c.getRiskLevel(),
                p.getId(), p.getName(), p.getOrganization().getId(), p.getOrganization().getName(),
                c.getSubmittedBy().getFullName(), c.getScript(), c.getCurrentStep(), c.getChain(),
                report, c.getCreatedAt(), c.getUpdatedAt());
    }

    public ReviewTaskResponse toTask(ReviewTask t) {
        return new ReviewTaskResponse(t.getId(), t.getStep(), t.getRequiredRole(), t.getStatus(),
                toContent(t.getContentItem()), t.getCreatedAt());
    }
}
