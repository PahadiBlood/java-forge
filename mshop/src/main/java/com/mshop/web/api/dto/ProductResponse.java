package com.mshop.web.api.dto;

public record ProductResponse(
        String id,
        String name,
        String category,
        String description,
        double price,
        String imageUrl
) {
}
