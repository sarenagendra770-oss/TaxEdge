package com.taxedge.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {
    /** 10-digit Indian mobile number. */
    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "mobile must be 10 digits")
    private String mobile;
}
