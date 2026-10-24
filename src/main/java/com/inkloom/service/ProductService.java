package com.inkloom.service;

import com.inkloom.dto.Dtos.*;
import com.inkloom.entity.Organization;
import com.inkloom.entity.Product;
import com.inkloom.entity.User;
import com.inkloom.enums.OrgRole;
import com.inkloom.exception.ApiException;
import com.inkloom.repository.OrganizationRepository;
import com.inkloom.repository.ProductRepository;
import com.inkloom.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Company product catalog: what the product is and which claims are allowed / forbidden. */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository products;
    private final OrganizationRepository orgs;
    private final AccessService access;
    private final AuditService audit;
    private final MapperService mapper;

    public ProductService(ProductRepository products, OrganizationRepository orgs, AccessService access,
                          AuditService audit, MapperService mapper) {
        this.products = products; this.orgs = orgs; this.access = access; this.audit = audit; this.mapper = mapper;
    }

    @Transactional
    public ProductResponse create(Long actorId, Long orgId, CreateProductRequest req) {
        User actor = access.requireUser(actorId);
        access.requireAnyRole(orgId, actorId, OrgRole.ORG_ADMIN, OrgRole.MARKETING_MANAGER);
        Organization org = orgs.findById(orgId).orElseThrow(() -> ApiException.notFound("Organization not found"));

        Product p = products.save(new Product(org, InputSanitizer.plain(req.name()), InputSanitizer.plain(req.brand()),
                InputSanitizer.plain(req.description()), cleanSet(req.approvedClaims()), cleanSet(req.forbiddenClaims())));
        audit.write(actor, orgId, "PRODUCT_CREATED", "Product", p.getId(), "name=" + p.getName());
        log.info("[PRODUCT] created productId={} '{}' org={} approved={} forbidden={}", p.getId(), p.getName(), orgId,
                p.getApprovedClaims().size(), p.getForbiddenClaims().size());
        return mapper.toProduct(p);
    }

    /** Any logged-in user can search the catalog (creators need it). Max 25 results. */
    @Transactional(readOnly = true)
    public List<ProductResponse> search(String q) {
        String term = q == null ? "" : InputSanitizer.plain(q).replace("%", "").replace("_", "");
        if (term.length() > 100) term = term.substring(0, 100);
        List<ProductResponse> out = new ArrayList<>();
        for (Product p : products.search(term, PageRequest.of(0, 25))) out.add(mapper.toProduct(p));
        log.debug("[PRODUCT] search '{}' -> {} result(s)", term, out.size());
        return out;
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return mapper.toProduct(products.findByIdAndActiveTrue(id).orElseThrow(() -> ApiException.notFound("Product not found")));
    }

    private Set<String> cleanSet(List<String> in) {
        Set<String> out = new LinkedHashSet<>();
        if (in != null) for (String s : in) {
            String c = InputSanitizer.plain(s);
            if (c != null && !c.isBlank()) out.add(c);
        }
        return out;
    }
}
