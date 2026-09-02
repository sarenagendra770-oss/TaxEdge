package com.taxedge.modules.insurance.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "insurance_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Policy extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false, length = 20)
    private String policyType; // LIFE, HEALTH, VEHICLE, TERM

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @Column(precision = 18, scale = 2)
    private BigDecimal sumAssured;

    @Column(precision = 18, scale = 2)
    private BigDecimal premium;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, LAPSED, CLAIMED, EXPIRED

    private String remarks;
}
