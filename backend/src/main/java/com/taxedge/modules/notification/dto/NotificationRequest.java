package com.taxedge.modules.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {
    @NotNull private Long userId;
    @NotBlank private String title;
    @NotBlank private String message;
    private String type;
}
