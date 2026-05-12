package com.example.un.models;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Setter
@Getter
@Table(name = "cumulative")
public class Cumulative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount",precision = 15,scale = 5)
    private BigDecimal amount;

    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Column(name = "create_at", updatable = false)
    private LocalDate createAt = LocalDate.now();

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;
}
