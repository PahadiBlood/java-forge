package com.mshop.web.api.dto;

import java.util.Date;

public record ReviewResponse(
        String id,
        String productId,
        String author,
        double rating,
        String comment,
        Date createdAt
) {
}
