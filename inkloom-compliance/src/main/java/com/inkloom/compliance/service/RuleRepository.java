package com.inkloom.compliance.service;

import com.inkloom.compliance.model.Rule;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RuleRepository extends MongoRepository<Rule, String> {
    List<Rule> findByActiveTrue();
}
