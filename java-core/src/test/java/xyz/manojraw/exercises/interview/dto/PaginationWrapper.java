package xyz.manojraw.exercises.interview.dto;

import java.util.List;

public record PaginationWrapper<T>(
        List<T> data,
        int currentPage,
        int pageSize, //user requested page
        int totalPages, // Math.ceil(totalItems/pageSize)
        long totalItems
) {
}
