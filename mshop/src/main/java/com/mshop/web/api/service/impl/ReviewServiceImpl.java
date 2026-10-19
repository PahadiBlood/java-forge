package com.mshop.web.api.service.impl;

import com.mshop.web.api.db.DataCenter;
import com.mshop.web.api.dto.ProductReviewSummary;
import com.mshop.web.api.dto.ReviewResponse;
import com.mshop.web.api.dto.ReviewWrapper;
import com.mshop.web.api.modal.Review;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
