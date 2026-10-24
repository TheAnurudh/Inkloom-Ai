package com.inkloom.repository;

import com.inkloom.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    /** Parameterised JPQL -> safe against SQL injection. */
    @Query("select p from Product p where p.active = true and (" +
           "lower(p.name) like lower(concat('%', :q, '%')) or " +
           "lower(p.brand) like lower(concat('%', :q, '%')) or " +
           "lower(p.organization.name) like lower(concat('%', :q, '%'))) order by p.name")
    List<Product> search(@Param("q") String q, Pageable pageable);
}
