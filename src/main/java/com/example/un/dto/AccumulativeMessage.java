package com.example.un.dto;

import com.example.un.models.OperationType;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.math.BigDecimal;

@Data
@Setter
@Getter
@RequiredArgsConstructor
public class AccumulativeMessage {
    private Long accountId;
    private BigDecimal amount;
    private OperationType operationType;
    private Long serviceId;
    private Long timestamp;
    private String referenceId;
}
