package com.finance.tracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CategoryExpenseStats {
    private String categoryName;
    private BigDecimal amount;
    private Double percentage;
}
