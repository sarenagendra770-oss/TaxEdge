package com.taxedge.modules.auth.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "otp_codes", indexes = @Index(columnList = "mobile"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpCode extends BaseEntity {

    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean consumed = false;
}
