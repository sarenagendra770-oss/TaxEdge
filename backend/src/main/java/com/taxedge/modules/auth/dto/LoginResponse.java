package com.taxedge.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private boolean success;
    /** true if the mobile already has a completed profile. */
    private boolean registered;
    /** Only populated when app.otp.mode=STUB, for local dev/testing. */
    private String devOtp;
    private String message;
}
