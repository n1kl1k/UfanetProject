package com.example.un.dto;

import com.example.un.models.OperationType;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@RequiredArgsConstructor
@Data
public class TransactionMessage {
    private Long accountId;
    private BigDecimal amount;
    private OperationType operationType;
    private Long timestamp;
    private String referenceId;
}
