package com.taxedge.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "mobile must be 10 digits")
    private String mobile;

    @NotBlank
    @Size(min = 6, max = 6, message = "otp must be 6 digits")
    private String otp;
}
