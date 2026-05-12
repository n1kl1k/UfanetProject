package com.example.un.dto;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Setter
@Getter
@RequiredArgsConstructor
public class ServiceDto {
    private Long serviceId;
    private String serviceName;
    private BigDecimal serviceCost;
}
