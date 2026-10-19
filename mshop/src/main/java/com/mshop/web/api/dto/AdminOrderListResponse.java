package com.mshop.web.api.dto;

import java.util.List;

public record AdminOrderListResponse(
        List<AdminOrderResponse> items,
        int page,
        int size,
        int totalPages,
        long totalItems
) {
}
