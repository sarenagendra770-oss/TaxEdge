package com.taxedge.modules.gst.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GstReturnRequest {
    @NotBlank
    private String gstin;
    @NotNull
    private Integer periodMonth;
    @NotNull
    private Integer periodYear;
    @NotBlank
    private String returnType;
    private BigDecimal totalTaxableValue;
    private BigDecimal totalTax;
    private String status;
    private LocalDate filedDate;
    private String remarks;
}
