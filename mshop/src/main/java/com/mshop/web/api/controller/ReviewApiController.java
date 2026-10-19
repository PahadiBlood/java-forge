package com.mshop.web.api.controller;

import com.mshop.web.api.dto.ReviewListRequest;
import com.mshop.web.api.dto.ReviewWrapper;
import com.mshop.web.api.service.impl.ReviewServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ReviewApiController {
    private final ReviewServiceImpl reviewServiceImpl;

    // find reviews by product id (paged)
    @GetMapping("/products/{id}/reviews")
    public ResponseEntity<ReviewWrapper> findReviewsByProductId(@PathVariable String id,
                                                                 @ModelAttribute ReviewListRequest request) {
        return ResponseEntity.ok(reviewServiceImpl.findReviewsByProductId(
                id, request.getPage(), request.getSize(), request.getSort()));
    }
}
