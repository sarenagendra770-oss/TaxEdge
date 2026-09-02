package com.taxedge.modules.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "mobile must be 10 digits")
    private String mobile;

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String customerType;

    private LocalDate dob;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN must be 10 chars, e.g. ABCDE1234F")
    private String pan;

    @Pattern(regexp = "^[0-9]{12}$", message = "aadhaar must be 12 digits")
    private String aadhaar;

    private String address;
    private String avatarUrl;

    /** Optional password. If provided, enables password-based re-login later. */
    @Size(min = 6, message = "password must be at least 6 chars")
    private String password;
}
