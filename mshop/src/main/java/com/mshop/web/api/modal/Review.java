package com.mshop.web.api.modal;

import lombok.Data;

import java.util.Date;

@Data
public class Review {
    private String id;
    private String productId;
    private String author;
    private double rating;
    private String comment;
    private Date createdAt;
}
