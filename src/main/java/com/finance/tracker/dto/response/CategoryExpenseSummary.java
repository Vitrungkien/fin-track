package com.finance.tracker.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CategoryExpenseSummary {
    private Long categoryId;
    private String categoryName;
    private String color;
    private String icon;
    private BigDecimal totalAmount;
    private Double percentage;
}
