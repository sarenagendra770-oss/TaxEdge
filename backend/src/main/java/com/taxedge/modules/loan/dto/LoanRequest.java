package com.taxedge.modules.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanRequest {
    @NotBlank private String loanType;
    @NotNull @Positive private BigDecimal amount;
    @NotNull @Positive private Integer tenureMonths;
    private BigDecimal interestRate;
    private BigDecimal emi;
    private String purpose;
    private String status;
    private String remarks;
}
