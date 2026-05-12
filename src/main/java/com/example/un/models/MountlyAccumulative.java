package com.example.un.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name = "mounthly_accumulative", uniqueConstraints =
        {@UniqueConstraint(columnNames = {"accountId", "serviceId", "month_year"})})
public class MountlyAccumulative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long accountId;
    private Long serviceId;
    private BigDecimal value;
    private BigDecimal tarif;
    private LocalDate monthYear;

}
