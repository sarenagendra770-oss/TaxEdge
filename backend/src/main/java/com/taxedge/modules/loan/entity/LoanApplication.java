package com.taxedge.modules.loan.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanApplication extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String loanType; // HOME, PERSONAL, AUTO, EDUCATION, BUSINESS

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(precision = 6, scale = 3)
    private BigDecimal interestRate;

    @Column(precision = 18, scale = 2)
    private BigDecimal emi;

    private String purpose;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, DISBURSED

    private String remarks;
}
