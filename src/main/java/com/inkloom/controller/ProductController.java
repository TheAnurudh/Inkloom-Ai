package com.inkloom.controller;

import com.inkloom.dto.Dtos.ProductResponse;
import com.inkloom.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Product catalog for creators (login required). */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService products;
    public ProductController(ProductService products) { this.products = products; }

    @GetMapping
    public List<ProductResponse> search(@RequestParam(required = false) String q) { return products.search(q); }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) { return products.get(id); }
}
