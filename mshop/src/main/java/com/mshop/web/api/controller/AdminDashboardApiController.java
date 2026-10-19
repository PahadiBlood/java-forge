package com.mshop.web.api.controller;

import com.mshop.web.api.dto.AdminOrderListRequest;
import com.mshop.web.api.dto.AdminOrderListResponse;
import com.mshop.web.api.dto.AdminOrderResponse;
import com.mshop.web.api.dto.DashboardSummaryRequest;
import com.mshop.web.api.dto.DashboardSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardApiController {

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @ModelAttribute DashboardSummaryRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/orders")
    public ResponseEntity<AdminOrderListResponse> findOrders(@ModelAttribute AdminOrderListRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<AdminOrderResponse> findOrderById(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
