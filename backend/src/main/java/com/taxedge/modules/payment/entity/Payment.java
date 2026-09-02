package com.taxedge.modules.payment.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(nullable = false)
    private String purpose;

    @Column(length = 30)
    @Builder.Default
    private String provider = "STUB";

    @Column(unique = true)
    private String transactionId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SUCCESS, FAILED, REFUNDED

    private String remarks;
}
