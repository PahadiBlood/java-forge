package com.mshop.web.api.modal;

import lombok.Data;

@Data
public class Product {
    private String id;
    private String name;
    private String category;
    private String description;
    private double price;
    private String imageUrl;
}
