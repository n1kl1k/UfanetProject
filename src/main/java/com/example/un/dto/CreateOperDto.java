package com.example.un.dto;


import com.example.un.models.OperationType;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Setter
@Getter
@RequiredArgsConstructor
public class CreateOperDto {
    private Long accountId;
    private Long operationId;
    private BigDecimal amount;
    private OperationType operationType;
    private LocalDateTime createdAt;
    private Long timeStamp;
    private String referenceId;

    private BigDecimal balance;

}
