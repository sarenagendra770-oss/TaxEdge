package com.taxedge.modules.auth.controller;

import com.taxedge.core.common.ApiResponse;
import com.taxedge.modules.auth.dto.*;
import com.taxedge.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** Step 1: mobile-only login → generates OTP, returns whether profile exists. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    /** Legacy alias so /auth/otp/send can be used from clients. */
    @PostMapping("/otp/send")
    public LoginResponse sendOtp(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    /** Step 2: verify OTP → issues JWT + returns whether profile is complete. */
    @PostMapping("/otp/verify")
    public ApiResponse<AuthResponse> verify(@Valid @RequestBody VerifyOtpRequest req) {
        return ApiResponse.ok("Verified", service.verifyOtp(req));
    }

    /** Step 3: fill/replace profile fields for the OTP-verified mobile. */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok("Registered", service.register(req));
    }
}
