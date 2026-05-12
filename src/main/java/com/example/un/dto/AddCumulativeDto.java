package com.example.un.dto;

import com.example.un.models.Operation;
import com.example.un.models.OperationType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddCumulativeDto {
    private Long accountId;
    private Long cumulativeId;
    private Long serviceId;
    private BigDecimal serviceCost;
    private LocalDate createAt;
    private BigDecimal amount;
    private OperationType operationType;
    private BigDecimal balance;
}
