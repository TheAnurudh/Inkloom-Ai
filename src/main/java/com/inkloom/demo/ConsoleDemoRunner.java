package com.inkloom.demo;

import com.inkloom.dto.Dtos.*;
import com.inkloom.entity.User;
import com.inkloom.enums.*;
import com.inkloom.repository.UserRepository;
import com.inkloom.exception.ApiException;
import com.inkloom.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * CONSOLE DEMO (no frontend needed).
 * On startup it plays the complete InkLoom story through the real services and prints every step,
 * including attacks that MUST be blocked. If you ever see "NOT BLOCKED", you found a security bug.
 * Turn off with:  inkloom.demo.enabled=false   (application.properties)
 */
@Component
@ConditionalOnProperty(name = "inkloom.demo.enabled", havingValue = "true")
public class ConsoleDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConsoleDemoRunner.class);
    private static final String PW = "Demo@12345";

    private static final String BAD_SCRIPT =
            "VitaBoost cures diabetes! 100% guaranteed results, 95% people love it, best in the world. No side effects.";
    private static final String GOOD_SCRIPT =
            "Loving my morning VitaBoost Juice! It contains 20% daily Vitamin C, as stated by the brand. #ad";

    private final AuthService auth;
    private final OrganizationService orgs;
    private final ProductService products;
    private final ContentService contents;
    private final ReviewService reviews;
    private final UserRepository userRepo;
    private final MapperService mapper;

    public ConsoleDemoRunner(AuthService auth, OrganizationService orgs, ProductService products,
                             ContentService contents, ReviewService reviews,
                             UserRepository userRepo, MapperService mapper) {
        this.auth = auth; this.orgs = orgs; this.products = products; this.contents = contents; this.reviews = reviews;
        this.userRepo = userRepo; this.mapper = mapper;
    }

    @Override
    public void run(String... args) {
        String stamp = String.valueOf(System.currentTimeMillis() % 100_000_000L);
        try {
            banner("INKLOOM DEMO START (stamp " + stamp + ")");

            // ---------- 1. accounts ----------
            banner("1. Register users");
            UserResponse admin = reg("Acme Admin", "admin." + stamp + "@inkloom.test", AccountType.COMPANY);
            UserResponse manager = reg("Maya Manager", "manager." + stamp + "@inkloom.test", AccountType.COMPANY);
            UserResponse brand = reg("Bhavna Brand", "brand." + stamp + "@inkloom.test", AccountType.COMPANY);
            UserResponse legal = reg("Leo Legal", "legal." + stamp + "@inkloom.test", AccountType.COMPANY);
            UserResponse influencer = reg("Ira Influencer", "ira." + stamp + "@inkloom.test", AccountType.INFLUENCER);
            UserResponse outsider = reg("Oscar Outsider", "oscar." + stamp + "@inkloom.test", AccountType.STARTUP, false);
            final UserResponse oscar = outsider;
            expectBlocked("Login BEFORE email verification", () -> auth.login(new LoginRequest(oscar.email(), PW)));
            verifyNow(outsider.email());
            say("Oscar verified his email (demo shortcut) - real users click the link from the email");

            AuthResponse login = auth.login(new LoginRequest(admin.email(), PW));
            say("Login OK for admin. Token starts with: " + login.accessToken().substring(0, 20) + "... (length " + login.accessToken().length() + ")");

            // ---------- 2. organization ----------
            banner("2. Company workspace");
            OrganizationResponse org = orgs.createOrganization(admin.id(), new CreateOrganizationRequest("Acme Foods " + stamp, "Demo company"));
            TeamResponse team = orgs.createTeam(admin.id(), org.id(), new CreateTeamRequest("Marketing"));
            orgs.addMember(admin.id(), org.id(), new AddMemberRequest(manager.email(), OrgRole.MARKETING_MANAGER, team.id()));
            orgs.addMember(admin.id(), org.id(), new AddMemberRequest(brand.email(), OrgRole.BRAND_REVIEWER, team.id()));
            orgs.addMember(admin.id(), org.id(), new AddMemberRequest(legal.email(), OrgRole.LEGAL_REVIEWER, null));
            orgs.upsertRule(admin.id(), org.id(), new ApprovalRuleRequest(ContentType.PRODUCT_ADVERTISEMENT, OrgRole.MARKETING_MANAGER, OrgRole.BRAND_REVIEWER));
            say("Members: " + orgs.listMembers(admin.id(), org.id()).stream().map(m -> m.fullName() + "=" + m.role()).toList());
            say("Rules  : " + orgs.listRules(admin.id(), org.id()));

            ProductResponse product = products.create(admin.id(), org.id(), new CreateProductRequest(
                    "VitaBoost Juice", "VitaBoost", "Orange juice with added vitamins",
                    List.of("Contains 20% daily Vitamin C"), List.of("cures diabetes", "no sugar")));
            say("Product listed: " + product.name() + " | approved=" + product.approvedClaims() + " | forbidden=" + product.forbiddenClaims());
            say("Creator search 'vita' finds: " + products.search("vita").size() + " product(s)");

            // ---------- 3. instant AI check ----------
            banner("3. Influencer: INSTANT AI CHECK of a risky script");
            AiReportResponse quick = contents.analyzeOnly(influencer.id(), new AnalyzeRequest(product.id(), ContentType.PRODUCT_ADVERTISEMENT, BAD_SCRIPT));
            printReport(quick);

            // ---------- 4. risky content goes through manager + legal ----------
            banner("4. Create + submit risky content (HIGH risk -> Manager then LEGAL)");
            ContentResponse c = contents.create(influencer.id(), new CreateContentRequest("VitaBoost reel", product.id(), ContentType.PRODUCT_ADVERTISEMENT, BAD_SCRIPT));
            say("Content #" + c.id() + " status=" + c.status() + " risk=" + c.riskLevel());
            c = contents.submit(influencer.id(), c.id());
            say("Submitted. status=" + c.status() + " chain=" + c.approvalChain());

            ReviewTaskResponse managerTask = firstPending(manager.id());
            say("Manager sees task #" + managerTask.taskId() + " for '" + managerTask.content().title() + "'");

            banner("5. SECURITY: attacks that must be BLOCKED");
            final Long mt = managerTask.taskId();
            final Long cid = c.id();
            expectBlocked("Influencer approves a task that is not hers", () -> reviews.decide(influencer.id(), mt, new ReviewDecisionRequest(Decision.APPROVE, null)));
            expectBlocked("Brand reviewer approves the manager's task", () -> reviews.decide(brand.id(), mt, new ReviewDecisionRequest(Decision.APPROVE, null)));
            expectBlocked("Outsider reads the content", () -> contents.getById(outsider.id(), cid));
            expectBlocked("Outsider lists the company members", () -> orgs.listMembers(outsider.id(), org.id()));
            expectBlocked("Manager (not admin) adds an employee", () -> orgs.addMember(manager.id(), org.id(), new AddMemberRequest(outsider.email(), OrgRole.LEGAL_REVIEWER, null)));
            expectBlocked("Outsider reads the audit log", () -> orgs.auditLogs(outsider.id(), org.id(), 0, 10));
            expectBlocked("Outsider edits influencer's content", () -> contents.update(outsider.id(), cid, new UpdateContentRequest(null, GOOD_SCRIPT)));
            expectBlocked("Manager rejects WITHOUT a comment", () -> reviews.decide(manager.id(), mt, new ReviewDecisionRequest(Decision.REJECT, "  ")));
            for (int i = 1; i <= 5; i++) {
                final int n = i;
                expectBlocked("Wrong password attempt " + n + " for Oscar", () -> auth.login(new LoginRequest(outsider.email(), "Wrong@" + n + "Pass")));
            }
            expectBlocked("Correct password but account is now LOCKED", () -> auth.login(new LoginRequest(outsider.email(), PW)));

            // ---------- 6. review: approve -> legal asks for changes ----------
            banner("6. Review workflow");
            sayResult(reviews.decide(manager.id(), mt, new ReviewDecisionRequest(Decision.APPROVE, "Looks fine for legal check")));
            ReviewTaskResponse legalTask = firstPending(legal.id());
            say("Legal sees task #" + legalTask.taskId() + " risk=" + legalTask.content().riskLevel());
            sayResult(reviews.decide(legal.id(), legalTask.taskId(), new ReviewDecisionRequest(Decision.REQUEST_CHANGES, "Remove the cure, 100% and guarantee claims")));

            // ---------- 7. creator fixes and resubmits ----------
            banner("7. Influencer fixes the script and resubmits");
            c = contents.update(influencer.id(), c.id(), new UpdateContentRequest(null, GOOD_SCRIPT));
            printReport(c.latestReport());
            c = contents.submit(influencer.id(), c.id());
            say("Resubmitted. status=" + c.status() + " chain=" + c.approvalChain());

            sayResult(reviews.decide(manager.id(), firstPending(manager.id()).taskId(), new ReviewDecisionRequest(Decision.APPROVE, "Good")));
            sayResult(reviews.decide(brand.id(), firstPending(brand.id()).taskId(), new ReviewDecisionRequest(Decision.APPROVE, "On brand")));

            ContentResponse done = contents.getById(influencer.id(), c.id());
            banner("8. FINAL STATUS of content #" + done.id() + " = " + done.status());

            banner("9. Audit trail (newest first)");
            orgs.auditLogs(admin.id(), org.id(), 0, 50).forEach(a ->
                    say(a.createdAt() + " | " + a.actorEmail() + " | " + a.action() + " | " + a.entityType() + "#" + a.entityId() + " | " + a.details()));

            banner("DEMO FINISHED. Server is still running on http://localhost:8080  (try GET /api/health)");
        } catch (Exception e) {
            log.error("[DEMO] failed - read the stack trace below to find the bug", e);
        }
    }

    // ------------------------------------------------------------ helpers

    private UserResponse reg(String name, String email, AccountType type) { return reg(name, email, type, true); }

    private UserResponse reg(String name, String email, AccountType type, boolean verify) {
        MessageResponse m = auth.register(new RegisterRequest(name, email, PW, type));
        if (verify) verifyNow(email);                    // DEMO SHORTCUT only (this class exists only when demo is enabled)
        User u = userRepo.findByEmail(email).orElseThrow();
        UserResponse r = mapper.toUser(u);
        say("registered " + r.fullName() + " <" + r.email() + "> id=" + r.id() + " type=" + r.accountType()
                + " verified=" + u.isEmailVerified() + " | api said: " + m.message());
        return r;
    }

    private void verifyNow(String email) {
        User u = userRepo.findByEmail(email).orElseThrow();
        u.markEmailVerified();
        userRepo.save(u);
    }

    private ReviewTaskResponse firstPending(Long reviewerId) {
        List<ReviewTaskResponse> tasks = reviews.myTasks(reviewerId, TaskStatus.PENDING);
        if (tasks.isEmpty()) throw new IllegalStateException("Demo expected a pending task for userId=" + reviewerId);
        return tasks.get(0);
    }

    private void expectBlocked(String label, Supplier<Object> action) {
        try {
            action.get();
            System.out.println("   !!! NOT BLOCKED (SECURITY BUG): " + label);
        } catch (ApiException | AuthenticationException e) {
            System.out.println("   [BLOCKED] " + label + "  ->  " + e.getMessage());
        }
    }

    private void printReport(AiReportResponse r) {
        System.out.println("   AI REPORT: risk=" + r.riskLevel() + " score=" + r.riskScore() + " meetsCriteria=" + r.meetsCriteria() + " source=" + r.source());
        System.out.println("   Summary  : " + r.summary());
        for (RedFlagResponse f : r.flags()) {
            System.out.println("   - [" + f.severity() + "] " + f.issue());
            System.out.println("       why : " + f.reason());
            System.out.println("       fix : " + f.suggestedAction());
        }
    }

    private void sayResult(DecisionResult d) { say("decision " + d.decision() + " -> content " + d.contentStatus() + " | " + d.message()); }
    private void say(String s) { System.out.println("   " + s); }
    private void banner(String s) { System.out.println("\n==================== " + s + " ===================="); }
}
