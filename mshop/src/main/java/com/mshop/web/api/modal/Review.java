package com.mshop.web.api.modal;

import lombok.Data;

import java.time.Instant;

@Data
public class Review {
    private String id;
    private String productId;
    private String author;
    private double rating;
    private String comment;
    private Instant createdAt;
}
