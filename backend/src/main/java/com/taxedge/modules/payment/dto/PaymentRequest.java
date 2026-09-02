package com.taxedge.modules.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotNull @Positive private BigDecimal amount;
    private String currency;
    @NotBlank private String purpose;
    private String provider;
    private String remarks;
}
