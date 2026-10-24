package com.inkloom.controller;

import com.inkloom.dto.Dtos.*;
import com.inkloom.enums.TaskStatus;
import com.inkloom.security.CustomUserDetails;
import com.inkloom.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Company reviewer side. */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviews;
    public ReviewController(ReviewService reviews) { this.reviews = reviews; }

    @GetMapping("/my-tasks")
    public List<ReviewTaskResponse> myTasks(@AuthenticationPrincipal CustomUserDetails me,
                                            @RequestParam(defaultValue = "PENDING") TaskStatus status) {
        return reviews.myTasks(me.getId(), status);
    }

    @GetMapping("/tasks/{taskId}")
    public ReviewTaskResponse task(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long taskId) {
        return reviews.getTask(me.getId(), taskId);
    }

    @PostMapping("/tasks/{taskId}/decision")
    public DecisionResult decide(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long taskId,
                                 @Valid @RequestBody ReviewDecisionRequest req) {
        return reviews.decide(me.getId(), taskId, req);
    }
}
