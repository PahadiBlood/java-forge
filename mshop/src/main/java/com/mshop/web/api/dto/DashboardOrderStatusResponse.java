package com.mshop.web.api.dto;

public record DashboardOrderStatusResponse(
        String status,
        long orderCount
) {
}
