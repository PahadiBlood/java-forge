package com.mshop.web.api.dto;

import java.time.Instant;

public record ReviewResponse(
        String id,
        String productId,
        String author,
        double rating,
        String comment,
        Instant createdAt
) {
}
