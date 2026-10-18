package com.mshop.web.api.dto;

import java.util.List;

public record ProductSearchWrapper(
        List<ProductResponse> items,
        int page,
        int size,
        int totalPages,
        long totalItems
) {
}
