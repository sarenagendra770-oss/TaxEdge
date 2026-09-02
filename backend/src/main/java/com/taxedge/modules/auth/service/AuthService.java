package com.taxedge.modules.auth.service;

import com.taxedge.core.exception.ApiException;
import com.taxedge.core.security.JwtService;
import com.taxedge.modules.auth.dto.*;
import com.taxedge.modules.auth.entity.OtpCode;
import com.taxedge.modules.auth.repository.OtpCodeRepository;
import com.taxedge.modules.user.dto.UserDTO;
import com.taxedge.modules.user.entity.Role;
import com.taxedge.modules.user.entity.User;
import com.taxedge.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepo;
    private final OtpCodeRepository otpRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @Value("${app.otp.mode:STUB}")
    private String otpMode;

    @Value("${app.otp.stub-code:123456}")
    private String stubCode;

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    public AuthService(UserRepository userRepo, OtpCodeRepository otpRepo,
                       PasswordEncoder encoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.otpRepo = otpRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    /**
     * Step 1: mobile-only "login". Creates a shell user record if none exists,
     * generates & stores a fresh OTP, and reports whether the profile is complete.
     * In STUB mode the OTP is returned in the response for local testing.
     */
    public LoginResponse login(LoginRequest req) {
        String mobile = req.getMobile();
        User user = userRepo.findByMobile(mobile).orElseGet(() -> userRepo.save(User.builder()
                .mobile(mobile).role(Role.USER).enabled(true).profileComplete(false).build()));

        String code = "STUB".equalsIgnoreCase(otpMode)
                ? stubCode
                : String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        otpRepo.save(OtpCode.builder()
                .mobile(mobile).code(code)
                .expiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES))
                .consumed(false).build());

        log.info("OTP for {} = {} (mode={})", mobile, code, otpMode);

        String devOtp = "STUB".equalsIgnoreCase(otpMode) ? code : null;
        return new LoginResponse(true, user.isProfileComplete(), devOtp, "OTP sent");
    }

    /**
     * Step 2: verify OTP and issue a JWT.
     * If the profile isn't complete yet, `registered=false` so the client
     * routes to the profile-completion screen.
     */
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        OtpCode otp = otpRepo.findFirstByMobileAndConsumedFalseOrderByCreatedAtDesc(req.getMobile())
                .orElseThrow(() -> new ApiException("No OTP requested for this mobile", HttpStatus.BAD_REQUEST));

        if (otp.getExpiresAt().isBefore(Instant.now()))
            throw new ApiException("OTP expired", HttpStatus.BAD_REQUEST);
        if (!otp.getCode().equals(req.getOtp()))
            throw new ApiException("Invalid OTP", HttpStatus.UNAUTHORIZED);

        otp.setConsumed(true);
        otpRepo.save(otp);

        User user = userRepo.findByMobile(req.getMobile())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        String token = jwtService.generate(user.getMobile(),
                Map.of("role", user.getRole().name(), "uid", user.getId()));
        return new AuthResponse(token, UserDTO.from(user), user.isProfileComplete());
    }

    /**
     * Alternative login for return users who set a password during /auth/register.
     * Returns the same JWT as the OTP flow. 401 on missing/incorrect password.
     */
    public AuthResponse passwordLogin(PasswordLoginRequest req) {
        User user = userRepo.findByMobile(req.getMobile())
                .orElseThrow(() -> new ApiException("Invalid mobile or password", HttpStatus.UNAUTHORIZED));
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ApiException("No password set for this account — use OTP login", HttpStatus.UNAUTHORIZED);
        }
        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid mobile or password", HttpStatus.UNAUTHORIZED);
        }
        if (!user.isEnabled()) {
            throw new ApiException("Account disabled", HttpStatus.FORBIDDEN);
        }
        String token = jwtService.generate(user.getMobile(),
                Map.of("role", user.getRole().name(), "uid", user.getId()));
        return new AuthResponse(token, UserDTO.from(user), user.isProfileComplete());
    }

    /**
     * Step 3 (post-OTP): complete the profile. Upserts by mobile.
     */
    public AuthResponse register(RegisterRequest req) {
        User user = userRepo.findByMobile(req.getMobile())
                .orElseGet(() -> User.builder().mobile(req.getMobile()).role(Role.USER).enabled(true).build());

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepo.existsByEmail(req.getEmail())) {
            throw new ApiException("Email already registered", HttpStatus.CONFLICT);
        }

        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setCustomerType(req.getCustomerType());
        user.setDob(req.getDob());
        user.setPan(req.getPan() == null ? null : req.getPan().toUpperCase());
        user.setAadhaar(req.getAadhaar());
        user.setAddress(req.getAddress());
        user.setAvatarUrl(req.getAvatarUrl());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(encoder.encode(req.getPassword()));
        }
        user.setProfileComplete(true);
        user = userRepo.save(user);

        String token = jwtService.generate(user.getMobile(),
                Map.of("role", user.getRole().name(), "uid", user.getId()));
        return new AuthResponse(token, UserDTO.from(user), true);
    }
}
