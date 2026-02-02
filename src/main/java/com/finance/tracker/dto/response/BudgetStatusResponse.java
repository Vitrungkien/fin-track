package com.finance.tracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BudgetStatusResponse {
    private Long budgetId;
    private String categoryName;
    private String categoryColor;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private Double percentage;
    private boolean isExceeded;
}
