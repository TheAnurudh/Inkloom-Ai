package com.inkloom.compliance.web;

import com.inkloom.compliance.model.ComplianceReport;
import com.inkloom.compliance.model.ComplianceRequest;
import com.inkloom.compliance.model.Rule;
import com.inkloom.compliance.service.ComplianceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final ComplianceService service;

    public ComplianceController(ComplianceService service) { this.service = service; }

    /** Main endpoint: send a script, get back score + flags + suggestions. */
    @PostMapping("/check")
    public ComplianceReport check(@Valid @RequestBody ComplianceRequest request) {
        return service.check(request);
    }

    @GetMapping("/rules")
    public List<Rule> rules() { return service.listRules(); }

    /** Admin use: protect with your teammate's RBAC (e.g. ROLE_ADMIN) once Spring Security is wired in. */
    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Rule addRule(@RequestBody Rule rule) { return service.addRule(rule); }
}
