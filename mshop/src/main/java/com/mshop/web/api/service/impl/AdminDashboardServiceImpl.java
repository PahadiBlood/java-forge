package com.mshop.web.api.service.impl;

import com.mshop.web.api.db.DataCenter;
import com.mshop.web.api.dto.*;
import com.mshop.web.api.modal.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl {
    private final List<Order> orders;
    private final List<Product> products;
    private final List<User> users;

    public AdminDashboardServiceImpl(DataCenter dataCenter) {
        orders = dataCenter.getOrders();
        products = dataCenter.getProducts();
        users = dataCenter.getUsers();
    }

    public DashboardSummaryResponse getDashboardSummary(DashboardSummaryRequest request) {
        int days = Math.max(1, request.getDays());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(days);
        List<Order> recent = orders.stream().filter(o -> o.getCreatedAt() != null &&
                !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(now)).toList();
        Map<OrderStatus, Long> counts = recent.stream().filter(o -> o.getStatus() != null)
                .collect(Collectors.groupingBy(Order::getStatus, () -> new EnumMap<>(OrderStatus.class), Collectors.counting()));
        List<Order> delivered = recent.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).toList();
        BigDecimal netSales = recent.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.RETURNED)
                .map(Order::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deliveredTotal = delivered.stream().map(Order::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = delivered.isEmpty() ? BigDecimal.ZERO : deliveredTotal.divide(BigDecimal.valueOf(delivered.size()), 2, RoundingMode.HALF_UP);
        Map<String, String> categories = products.stream().collect(Collectors.toMap(Product::getId, Product::getCategory, (a,b) -> a));
        Map<String, BigDecimal> revenue = new TreeMap<>();
        recent.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.RETURNED)
                .filter(o -> o.getItems() != null).flatMap(o -> o.getItems().stream())
                .forEach(item -> revenue.merge(categories.getOrDefault(item.getProductId(), "Other"),
                        Objects.requireNonNullElse(item.getLineTotal(), BigDecimal.ZERO), BigDecimal::add));
        return new DashboardSummaryResponse(days, recent.size(), counts.getOrDefault(OrderStatus.PROCESSING, 0L), delivered.size(),
                counts.getOrDefault(OrderStatus.CANCELLED, 0L), netSales, average,
                Arrays.stream(OrderStatus.values()).map(s -> new DashboardOrderStatusResponse(s.name(), counts.getOrDefault(s, 0L).intValue())).toList(),
                revenue.entrySet().stream().map(e -> new DashboardCategoryRevenueResponse(e.getKey(), e.getValue())).toList(),
                topCustomers(recent));
    }

    private List<DashboardTopCustomerResponse> topCustomers(List<Order> recent) {
        Map<String, User> userById = users.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a,b) -> a));
        Map<String, List<Order>> byCustomer = recent.stream().filter(o -> o.getUserId() != null).collect(Collectors.groupingBy(Order::getUserId));
        return byCustomer.entrySet().stream().map(e -> {
            User user = userById.get(e.getKey());
            String name = user == null ? e.getKey() : user.getFirstName() + " " + user.getLastName();
            BigDecimal spend = e.getValue().stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.RETURNED)
                    .map(Order::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new DashboardTopCustomerResponse(name, e.getValue().size(), spend);
        }).sorted(Comparator.comparing(DashboardTopCustomerResponse::totalSpend).reversed()).limit(5).toList();
    }

    public AdminOrderListResponse findOrders(AdminOrderListRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.max(1, Math.min(request.getSize(), 100));
        String search = request.getSearch() == null ? "" : request.getSearch().trim().toLowerCase(Locale.ROOT);
        OrderStatus status = null;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try { status = OrderStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Unknown order status: " + request.getStatus()); }
        }
        OrderStatus selectedStatus = status;
        Map<String, User> userById = users.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a,b) -> a));
        Comparator<Order> comparator = Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        if (request.getSort() == null || !request.getSort().toLowerCase(Locale.ROOT).endsWith(",asc")) comparator = comparator.reversed();
        List<Order> matching = orders.stream().filter(o -> selectedStatus == null || o.getStatus() == selectedStatus)
                .filter(o -> search.isEmpty() || o.getId().toLowerCase(Locale.ROOT).contains(search) ||
                        Optional.ofNullable(userById.get(o.getUserId())).map(u -> (u.getFirstName()+" "+u.getLastName()+" "+u.getEmail()).toLowerCase(Locale.ROOT).contains(search)).orElse(false))
                .sorted(comparator).toList();
        int from = Math.min(page * size, matching.size());
        return new AdminOrderListResponse(matching.subList(from, Math.min(from + size, matching.size())).stream().map(this::toResponse).toList(),
                page, size, (int)Math.ceil((double)matching.size()/size), matching.size());
    }

    public AdminOrderResponse findOrderById(String id) {
        return orders.stream().filter(o -> Objects.equals(o.getId(), id)).findFirst().map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    private AdminOrderResponse toResponse(Order order) {
        User user = users.stream().filter(u -> Objects.equals(u.getId(), order.getUserId())).findFirst().orElse(null);
        List<AdminOrderItemResponse> items = order.getItems() == null ? List.of() : order.getItems().stream().map(item -> {
            Product product = products.stream().filter(p -> Objects.equals(p.getId(), item.getProductId())).findFirst().orElse(null);
            return new AdminOrderItemResponse(item.getProductId(), product == null ? "Unknown product" : product.getName(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }).toList();
        return new AdminOrderResponse(order.getId(), user == null ? "Unknown customer" : user.getFirstName()+" "+user.getLastName(),
                user == null ? "" : user.getEmail(), order.getStatus() == null ? "UNKNOWN" : order.getStatus().name(),
                order.getCreatedAt() == null ? null : order.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant(),
                order.getSubtotal(), order.getShipping(), order.getTotal(), items);
    }
}
