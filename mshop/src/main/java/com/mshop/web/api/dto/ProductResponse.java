package com.mshop.web.api.dto;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String name,
        String category,
        String description,
        BigDecimal price,
        String imageUrl
) {
}
