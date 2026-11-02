package com.mshop.web.api.modal;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItem {
    private String productId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
