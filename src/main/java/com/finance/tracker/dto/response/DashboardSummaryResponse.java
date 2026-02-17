package com.finance.tracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardSummaryResponse {
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance; // Monthly balance
    private BigDecimal cumulativeBalance; // Total balance until selected date
    private Long transactionCount;
}
