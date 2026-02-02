package com.finance.tracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ChartDataResponse {
    private List<String> labels;
    private List<BigDecimal> data;
    private List<String> colors;
}
