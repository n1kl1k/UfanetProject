package com.example.un.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Table(name = "service")
@Entity
public class ServiceInCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "service_cost", precision = 15, scale = 5, nullable = false)
    private BigDecimal serviceCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cumulative_id")
    private Cumulative cumulative;
}
