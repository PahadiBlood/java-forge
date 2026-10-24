package com.mshop.web.api.service.impl;

import com.mshop.web.api.db.DataCenter;
import com.mshop.web.api.dto.*;
import com.mshop.web.api.modal.Order;
import com.mshop.web.api.modal.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl {
    private final List<Order> orders;

    public AdminDashboardServiceImpl(DataCenter dataCenter) {
        this.orders = dataCenter.getOrders();
    }

    public DashboardSummaryResponse getDashboardSummary(DashboardSummaryRequest request) {
        int days = request.getDays();

        LocalDateTime today = LocalDateTime.now();
        LocalDateTime pastDateTime = LocalDateTime.now().minusDays(days);

        var filteredOrders = orders
                .stream()
                .filter(o ->
                        {
                            LocalDateTime createdAt = o.getCreatedAt();
                            return ((createdAt.equals(pastDateTime) || createdAt.isAfter(pastDateTime)) && (createdAt.equals(today) || createdAt.isBefore(today)));
                        }
                ).toList();

        long totalOrders = filteredOrders.size();
        var orderGroupByStatus = filteredOrders
                .stream()
                .collect(Collectors.groupingBy(Order::getStatus));

        long awaitingFulfillment = orderGroupByStatus.getOrDefault(OrderStatus.PROCESSING, Collections.emptyList()).size();
        long cancelOrders = orderGroupByStatus.getOrDefault(OrderStatus.CANCELLED, Collections.emptyList()).size();

        var deliveredOrders = orderGroupByStatus.get(OrderStatus.DELIVERED);
        long fulfilledOrders = deliveredOrders.size();

        //var summary = filteredOrders.stream().collect(Collectors.summarizingDouble(Order::getTotal));

        BigDecimal netSales = new BigDecimal(orderGroupByStatus
                .entrySet()
                .stream()
                .filter(e -> (OrderStatus.CANCELLED != e.getKey() && OrderStatus.RETURNED != e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .mapToDouble(Order::getTotal)
                .sum()
        );

        BigDecimal averageOrderValue = new BigDecimal(
                deliveredOrders
                        .stream()
                        .mapToDouble(Order::getTotal)
                        .average()
                        .orElse(0)
        );

        return new DashboardSummaryResponse(
                days, totalOrders,
                awaitingFulfillment,
                fulfilledOrders,
                cancelOrders,
                netSales,
                averageOrderValue,
                List.of(),
                List.of(),
                List.of()
        );
    }

    public AdminOrderListResponse findOrders(AdminOrderListRequest request) {
        return null;
    }

    public AdminOrderResponse findOrderById(String id) {
        return null;
    }
}
