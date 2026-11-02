package com.mshop.web.api.dto;

import java.math.BigDecimal;

public record OrderItemCategory(String category, BigDecimal lineTotal) {
}
