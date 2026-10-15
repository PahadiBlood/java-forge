package xyz.manojraw.exercises.interview.dto;

import java.math.BigDecimal;

public record CarUpdateRequest(
        String make,
        String model,
        BigDecimal price,
        int year,
        String color
) {
}
