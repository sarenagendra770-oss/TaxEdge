package com.taxedge.modules.loan.controller;

import com.taxedge.core.common.ApiResponse;
import com.taxedge.modules.loan.dto.LoanRequest;
import com.taxedge.modules.loan.entity.LoanApplication;
import com.taxedge.modules.loan.service.LoanService;
import com.taxedge.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService service;
    private final UserService userService;
    public LoanController(LoanService service, UserService userService) { this.service = service; this.userService = userService; }
    private Long uid(UserDetails p) { return userService.getByMobile(p.getUsername()).getId(); }
    private boolean isAdmin(UserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CA"));
    }

    @PostMapping public ApiResponse<LoanApplication> create(@AuthenticationPrincipal UserDetails p, @Valid @RequestBody LoanRequest r) { return ApiResponse.ok("Created", service.create(uid(p), r)); }
    @GetMapping public ApiResponse<List<LoanApplication>> mine(@AuthenticationPrincipal UserDetails p) { return ApiResponse.ok(service.listMine(uid(p))); }
    @GetMapping("/all") @PreAuthorize("hasAnyRole('ADMIN','CA')") public ApiResponse<List<LoanApplication>> all() { return ApiResponse.ok(service.listAll()); }
    @GetMapping("/{id}") public ApiResponse<LoanApplication> get(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) { return ApiResponse.ok(service.get(id, uid(p), isAdmin(p))); }
    @PutMapping("/{id}") public ApiResponse<LoanApplication> update(@AuthenticationPrincipal UserDetails p, @PathVariable Long id, @Valid @RequestBody LoanRequest r) { return ApiResponse.ok(service.update(id, uid(p), isAdmin(p), r)); }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','CA')")
    public ApiResponse<LoanApplication> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(service.updateStatus(id, body.get("status")));
    }
    @DeleteMapping("/{id}") public ApiResponse<String> delete(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) { service.delete(id, uid(p), isAdmin(p)); return ApiResponse.ok("Deleted", "OK"); }
}
