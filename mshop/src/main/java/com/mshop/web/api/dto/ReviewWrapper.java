package com.mshop.web.api.dto;

import java.util.List;

public record ReviewWrapper(
        List<ReviewResponse> items,
        int page,
        int size,
        int totalPages,
        long totalItems,
        ProductReviewSummary summary
) {
}
