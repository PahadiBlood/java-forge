package com.mshop.web.api.modal;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Product {
    private String id;
    private String name;
    private String category;
    private String description;
    private BigDecimal price;
    private String imageUrl;
}
