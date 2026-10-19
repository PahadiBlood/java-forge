package com.mshop.web.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        int days,
        long totalOrders,
        long awaitingFulfillment,
        long fulfilledOrders,
        long cancelledOrders,
        BigDecimal netSales,
        BigDecimal averageOrderValue,
        List<DashboardOrderStatusResponse> ordersByStatus,
        List<DashboardCategoryRevenueResponse> revenueByCategory,
        List<DashboardTopCustomerResponse> topCustomers
) {
}
