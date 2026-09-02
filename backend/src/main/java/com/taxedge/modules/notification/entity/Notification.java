package com.taxedge.modules.notification.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = "INFO"; // INFO, ALERT, REMINDER, SUCCESS

    @Column(nullable = false)
    @Builder.Default
    private boolean readFlag = false;
}
