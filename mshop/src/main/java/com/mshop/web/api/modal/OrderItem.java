package com.mshop.web.api.modal;

import lombok.Data;

@Data
public class OrderItem {
    private String productId;
    private int quantity;
    private double unitPrice;
    private double lineTotal;
}
