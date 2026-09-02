package com.taxedge.modules.itr.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "itr_returns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItrReturn extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String pan;

    @Column(nullable = false, length = 10)
    private String assessmentYear; // e.g. 2024-25

    @Column(nullable = false, length = 10)
    private String itrForm; // ITR1, ITR2, ITR3, ITR4

    @Column(precision = 18, scale = 2)
    private BigDecimal totalIncome;

    @Column(precision = 18, scale = 2)
    private BigDecimal taxLiability;

    @Column(precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, FILED, VERIFIED

    private LocalDate filedDate;

    private String remarks;
}
