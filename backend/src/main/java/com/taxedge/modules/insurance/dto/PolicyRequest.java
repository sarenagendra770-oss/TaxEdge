package com.taxedge.modules.insurance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PolicyRequest {
    @NotBlank private String provider;
    @NotBlank private String policyType;
    @NotBlank private String policyNumber;
    private BigDecimal sumAssured;
    private BigDecimal premium;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String remarks;
}
