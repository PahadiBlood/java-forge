package com.mshop.web.api.controller;

import com.mshop.web.api.dto.AdminOrderListRequest;
import com.mshop.web.api.dto.AdminOrderListResponse;
import com.mshop.web.api.dto.AdminOrderResponse;
import com.mshop.web.api.dto.DashboardSummaryRequest;
import com.mshop.web.api.dto.DashboardSummaryResponse;
import com.mshop.web.api.service.impl.AdminDashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardApiController {

    private final AdminDashboardServiceImpl dashboardServiceImpl;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @ModelAttribute DashboardSummaryRequest request) {
        return ResponseEntity.ok(dashboardServiceImpl.getDashboardSummary(request));
    }

    @GetMapping("/orders")
    public ResponseEntity<AdminOrderListResponse> findOrders(@ModelAttribute AdminOrderListRequest request) {
        return ResponseEntity.ok(dashboardServiceImpl.findOrders(request));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<AdminOrderResponse> findOrderById(@PathVariable String id) {
        return ResponseEntity.ok(dashboardServiceImpl.findOrderById(id));
    }
}
