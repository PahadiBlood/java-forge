package com.mshop.web.api.dto;

import java.math.BigDecimal;

public record DashboardCategoryRevenueResponse(
        String category,
        BigDecimal revenue
) {
}
