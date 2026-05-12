package com.example.un.dto;

import com.example.un.models.OperationType;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Setter
@Getter
@RequiredArgsConstructor
public class RefactorOperDto {
    private Long operationId;
    private BigDecimal amount;
    private OperationType operationType;

}
