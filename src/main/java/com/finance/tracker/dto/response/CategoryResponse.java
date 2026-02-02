package com.finance.tracker.dto.response;

import com.finance.tracker.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String color;
    private TransactionType type;
}
