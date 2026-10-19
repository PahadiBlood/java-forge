package com.mshop.web.api.controller;

import com.mshop.web.api.dto.HomeSection;
import com.mshop.web.api.dto.HomeSectionsRequest;
import com.mshop.web.api.dto.ProductListRequest;
import com.mshop.web.api.dto.ProductResponse;
import com.mshop.web.api.dto.ProductSearchRequest;
import com.mshop.web.api.dto.ProductSearchWrapper;
import com.mshop.web.api.dto.ProductWrapper;
import com.mshop.web.api.service.impl.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<List<HomeSection>> findHomeSections(@ModelAttribute HomeSectionsRequest request) {
        return ResponseEntity.ok(productServiceImpl.findHomeSections(request.getSize()));
    }

    // find all products (paged, optional category filter)
    @GetMapping("/products")
    public ResponseEntity<ProductWrapper> findAllProducts(@ModelAttribute ProductListRequest request) {
        return ResponseEntity.ok(productServiceImpl.findAllProducts(
                request.getCategory(), request.getPage(), request.getSize()));
    }

    @GetMapping("/products/search")
    public ResponseEntity<ProductSearchWrapper> searchProducts(@ModelAttribute ProductSearchRequest request) {
        return ResponseEntity.ok(productServiceImpl.searchProducts(
                request.getPage(), request.getSize(), request.getSort(), request.getSearch()));
    }

    // find product by id
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> findProductById(@PathVariable String id) {
        return ResponseEntity.ok(productServiceImpl.findProductById(id));
    }
}
