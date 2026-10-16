package com.mshop.web.api.dto;

import java.util.List;

public record HomeSection(
        String category,
        long totalItems,
        List<ProductResponse> items
) {
}
