package com.mshop.web.api.dto;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
