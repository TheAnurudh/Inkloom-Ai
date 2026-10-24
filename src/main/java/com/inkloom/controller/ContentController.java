package com.inkloom.controller;

import com.inkloom.dto.Dtos.*;
import com.inkloom.security.CustomUserDetails;
import com.inkloom.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Creator side: check script, save content, edit, submit to the company. */
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService content;
    public ContentController(ContentService content) { this.content = content; }

    @PostMapping("/analyze")
    public AiReportResponse analyze(@AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody AnalyzeRequest req) {
        return content.analyzeOnly(me.getId(), req);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse create(@AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody CreateContentRequest req) {
        return content.create(me.getId(), req);
    }

    @PutMapping("/{id}")
    public ContentResponse update(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id, @Valid @RequestBody UpdateContentRequest req) {
        return content.update(me.getId(), id, req);
    }

    @PostMapping("/{id}/submit")
    public ContentResponse submit(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) {
        return content.submit(me.getId(), id);
    }

    @GetMapping("/mine")
    public List<ContentResponse> mine(@AuthenticationPrincipal CustomUserDetails me) { return content.mine(me.getId()); }

    @GetMapping("/{id}")
    public ContentResponse get(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) { return content.getById(me.getId(), id); }
}
