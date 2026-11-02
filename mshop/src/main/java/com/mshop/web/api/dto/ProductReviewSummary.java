package com.mshop.web.api.dto;

import java.math.BigDecimal;

public record ProductReviewSummary(
        BigDecimal average,
        long count
) {
}
