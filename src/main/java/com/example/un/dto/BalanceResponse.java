package com.example.un.dto;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Setter
@Getter
public class BalanceResponse {
    private Long id;
    private BigDecimal balance;
    private LocalDate date;

    public BalanceResponse(Long id, BigDecimal balance, LocalDate date) {
        this.id = id;
        this.balance = balance;
        this.date = date;
    }
}
