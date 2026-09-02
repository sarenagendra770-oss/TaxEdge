package com.taxedge.modules.tds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TdsRequest {
    @NotBlank private String tan;
    @NotBlank private String deductorName;
    @NotBlank private String financialYear;
    @NotBlank private String quarter;
    @NotNull @Positive private BigDecimal tdsAmount;
    private String section;
    private String status;
    private String remarks;
}
