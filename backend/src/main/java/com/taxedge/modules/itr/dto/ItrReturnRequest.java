package com.taxedge.modules.itr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ItrReturnRequest {
    @NotBlank
    private String pan;
    @NotBlank
    private String assessmentYear;
    @NotBlank
    private String itrForm;
    private BigDecimal totalIncome;
    private BigDecimal taxLiability;
    private BigDecimal refundAmount;
    private String status;
    private LocalDate filedDate;
    private String remarks;
}
