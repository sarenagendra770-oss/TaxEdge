package com.taxedge.modules.tds.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tds_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TdsRecord extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String tan;

    @Column(nullable = false)
    private String deductorName;

    @Column(nullable = false, length = 10)
    private String financialYear; // 2024-25

    @Column(nullable = false, length = 5)
    private String quarter; // Q1..Q4

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal tdsAmount;

    @Column(length = 20)
    private String section; // e.g. 194C

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, FILED, VERIFIED

    private String remarks;
}
