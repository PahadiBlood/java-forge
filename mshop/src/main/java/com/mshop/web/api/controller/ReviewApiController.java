package com.mshop.web.api.controller;

import com.mshop.web.api.dto.ReviewWrapper;
import com.mshop.web.api.service.impl.ReviewServiceImpl;
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
public class ReviewApiController {
    private final ReviewServiceImpl reviewServiceImpl;

    // find reviews by product id (paged)
    @GetMapping("/products/{id}/reviews")
    public ResponseEntity<ReviewWrapper> findReviewsByProductId(@PathVariable String id,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(reviewServiceImpl.findReviewsByProductId(id, page, size));
    }
}
