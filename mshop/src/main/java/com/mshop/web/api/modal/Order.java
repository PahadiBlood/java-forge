package com.mshop.web.api.modal;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Order {
    private String id;
    private String userId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal subtotal;
    private BigDecimal shipping;
    private BigDecimal total;
    private List<OrderItem> items;
}
