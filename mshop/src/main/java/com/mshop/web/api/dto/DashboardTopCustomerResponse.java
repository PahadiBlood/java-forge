package com.mshop.web.api.dto;

import java.math.BigDecimal;

public record DashboardTopCustomerResponse(
        String customerName,
        long orderCount,
        BigDecimal totalSpend
) {
}
