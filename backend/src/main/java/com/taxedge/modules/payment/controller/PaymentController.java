package com.taxedge.modules.payment.controller;

import com.taxedge.core.common.ApiResponse;
import com.taxedge.modules.payment.dto.PaymentRequest;
import com.taxedge.modules.payment.entity.Payment;
import com.taxedge.modules.payment.service.PaymentService;
import com.taxedge.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;
    private final UserService userService;
    public PaymentController(PaymentService service, UserService userService) { this.service = service; this.userService = userService; }
    private Long uid(UserDetails p) { return userService.getByMobile(p.getUsername()).getId(); }
    private boolean isAdmin(UserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CA"));
    }

    @PostMapping("/initiate")
    public ApiResponse<Payment> initiate(@AuthenticationPrincipal UserDetails p, @Valid @RequestBody PaymentRequest r) {
        return ApiResponse.ok("Initiated", service.initiate(uid(p), r));
    }

    /** STUB webhook / manual confirmation (in prod, secure via provider signatures). */
    @PostMapping("/callback")
    public ApiResponse<Payment> callback(@RequestBody Map<String, String> body) {
        return ApiResponse.ok("Updated", service.markStatus(body.get("transactionId"), body.get("status")));
    }

    @GetMapping public ApiResponse<List<Payment>> mine(@AuthenticationPrincipal UserDetails p) { return ApiResponse.ok(service.listMine(uid(p))); }
    @GetMapping("/all") @PreAuthorize("hasAnyRole('ADMIN','CA')") public ApiResponse<List<Payment>> all() { return ApiResponse.ok(service.listAll()); }
    @GetMapping("/{id}") public ApiResponse<Payment> get(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) { return ApiResponse.ok(service.get(id, uid(p), isAdmin(p))); }
}
