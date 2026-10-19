package com.mshop.web.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminOrderResponse(
        String id,
        String customerName,
        String customerEmail,
        String status,
        Instant createdAt,
        BigDecimal subtotal,
        BigDecimal shipping,
        BigDecimal total,
        List<AdminOrderItemResponse> items
) {
}
