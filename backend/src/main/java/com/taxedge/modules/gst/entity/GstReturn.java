package com.taxedge.modules.gst.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "gst_returns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GstReturn extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 15)
    private String gstin;

    @Column(nullable = false)
    private Integer periodMonth;

    @Column(nullable = false)
    private Integer periodYear;

    @Column(nullable = false, length = 20)
    private String returnType; // GSTR1, GSTR3B, GSTR9

    @Column(precision = 18, scale = 2)
    private BigDecimal totalTaxableValue;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalTax;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, FILED, PENDING

    private LocalDate filedDate;

    private String remarks;
}
