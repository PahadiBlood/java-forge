package com.mshop.web.api.service.impl;

import com.mshop.web.api.db.DataCenter;
import com.mshop.web.api.dto.ProductReviewSummary;
import com.mshop.web.api.dto.ReviewResponse;
import com.mshop.web.api.dto.ReviewWrapper;
import com.mshop.web.api.modal.Review;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl {

    private final List<Review> reviews;

    public ReviewServiceImpl(DataCenter dataCenter) {
        reviews = dataCenter.getReviews();
    }

    public ReviewWrapper findReviewsByProductId(String id, int page, int size, String sort) {
        if (page < 0) page = 0;
        if (size < 1) size = 5;

        var reviewsByProductId = reviews
                .stream()
                .filter(r -> r.getProductId().equalsIgnoreCase(id))
                .sorted(reviewComparator(sort))
                .toList();

        var summary = reviewsByProductId
                .stream()
                .collect(Collectors.summarizingDouble(Review::getRating));

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


        return new ReviewWrapper(
                filteredReviewsByProductId, page, size, totalPages, totalItems,
                new ProductReviewSummary(summary.getAverage(), summary.getCount())
        );
    }

    private Comparator<Review> reviewComparator(String sort) {
        return switch (sort == null ? "newest" : sort.toLowerCase(Locale.ROOT)) {
            case "oldest" -> Comparator.comparing(
                    Review::getCreatedAt,
                    Comparator.nullsLast(Comparator.<Date>naturalOrder())
            );
            case "rating-desc" -> Comparator.comparingDouble(Review::getRating)
                    .reversed()
                    .thenComparing(Review::getCreatedAt,
                            Comparator.nullsLast(Comparator.<Date>reverseOrder()));
            case "rating-asc" -> Comparator.comparingDouble(Review::getRating)
                    .thenComparing(Review::getCreatedAt,
                            Comparator.nullsLast(Comparator.<Date>reverseOrder()));
            default -> Comparator.comparing(
                    Review::getCreatedAt,
                    Comparator.nullsLast(Comparator.<Date>reverseOrder())
            );
        };
    }
}
