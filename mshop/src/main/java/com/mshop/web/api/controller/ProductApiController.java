package com.mshop.web.api.controller;

import com.mshop.web.api.dto.HomeSection;
import com.mshop.web.api.dto.ProductResponse;
import com.mshop.web.api.dto.ProductSearchWrapper;
import com.mshop.web.api.dto.ProductWrapper;
import com.mshop.web.api.service.impl.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ProductApiController {

    private final ProductServiceImpl productServiceImpl;

    // find all categories
    @GetMapping("/categories")
    public ResponseEntity<List<String>> findAllCategories() {
        return ResponseEntity.ok(productServiceImpl.findAllCategories());
    }

    // home page rows: products grouped by category
    @GetMapping("/home/sections")
    public ResponseEntity<List<HomeSection>> findHomeSections(@RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productServiceImpl.findHomeSections(size));
    }

    // find all products (paged, optional category filter)
    @GetMapping("/products")
    public ResponseEntity<ProductWrapper> findAllProducts(@RequestParam(required = false) String category,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(productServiceImpl.findAllProducts(category, page, size));
    }

    @GetMapping("/products/search")
    public ResponseEntity<ProductSearchWrapper> searchProducts(@RequestParam(defaultValue = "") String search,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "15") int size,
                                                               @RequestParam(defaultValue = "relevance") String sort) {
        return ResponseEntity.ok(productServiceImpl.searchProducts(page, size, sort, search));
    }

    // find product by id
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> findProductById(@PathVariable String id) {
        return ResponseEntity.ok(productServiceImpl.findProductById(id));
    }
}
