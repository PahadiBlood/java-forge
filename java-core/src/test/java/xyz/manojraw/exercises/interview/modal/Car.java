package xyz.manojraw.exercises.interview.modal;

import java.math.BigDecimal;

public record Car(
        int id,
        String make,
        String model,
        BigDecimal price,
        int year,
        String color
) {
}
