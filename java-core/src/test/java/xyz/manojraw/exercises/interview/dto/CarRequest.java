package xyz.manojraw.exercises.interview.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record CarRequest(
        String make,
        String model,
        BigDecimal price,
        int year,
        String color
) {

    public CarRequest {
        make = validateMake(make);
        model = validateModel(model);
        price = validatePrice(price);
        year = validateYear(year);
        color = validateColor(color);
    }

    private String validateMake(String make) {
        if (isValidString(make)) {
            return make;
        }

        throw new IllegalArgumentException("Invalid value for Make");
    }

    private String validateModel(String model) {
        if (isValidString(model)) {
            return model;
        }

        throw new IllegalArgumentException("Invalid value for Model");
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (isValidPrice(price)) {
            return price;
        }

        throw new IllegalArgumentException("Invalid value for Price");
    }

    private int validateYear(int year) {
        if (isValidYear(year)) {
            return year;
        }

        throw new IllegalArgumentException("Invalid value for Year");
    }

    private String validateColor(String color) {
        if (isValidString(color)) {
            return color;
        }

        throw new IllegalArgumentException("Invalid value for Color");
    }

    private boolean isValidYear(int year) {
        // handle the time zone for real project by getting request timezone
        // 1769 was the year first car build
        return year > 0 && (year > 1768 && year <= LocalDate.now().getYear());
    }

    private boolean isValidPrice(BigDecimal price) {
        return Objects.nonNull(price) && price.compareTo(BigDecimal.ZERO) >= 0;
    }

    private boolean isValidString(String s) {
        return s != null && !s.isBlank();
    }
}
