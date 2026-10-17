package com.mshop.web.api.service.impl;

import com.mshop.web.api.db.DataCenter;
import com.mshop.web.api.dto.ProductReviewSummary;
import com.mshop.web.api.dto.ReviewResponse;
import com.mshop.web.api.dto.ReviewWrapper;
import com.mshop.web.api.modal.Review;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Service
public class ReviewServiceImpl {

    private final DataCenter dataCenter;
    private static List<Review> reviews;

    @PostConstruct
    void init() {
        // the problem with this data read and initialization is if data is too heavy
        // it will delay whole application startup

        var reviewsAsync = dataCenter.readReviewsAsync;
        try {
            reviews = reviewsAsync.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public ReviewWrapper findReviewsByProductId(String id, int page, int size) {

        var reviewsByProductId = reviews
                .stream()
                .filter(r -> r.getProductId().equalsIgnoreCase(id)).toList();

        var filteredReviewsByProductId = reviewsByProductId
                .stream()
                .skip((long) page * size)
                .limit(size)
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getProductId(),
                        r.getAuthor(),
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt()
                ))
                .toList();

        long totalItems = reviewsByProductId.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);


        return new ReviewWrapper(filteredReviewsByProductId, page, size, totalPages, totalItems, new ProductReviewSummary(4.5, 10));
    }
}