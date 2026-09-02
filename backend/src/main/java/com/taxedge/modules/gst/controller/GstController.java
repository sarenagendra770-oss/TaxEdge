package com.taxedge.modules.gst.controller;

import com.taxedge.core.common.ApiResponse;
import com.taxedge.modules.gst.dto.GstReturnRequest;
import com.taxedge.modules.gst.entity.GstReturn;
import com.taxedge.modules.gst.service.GstService;
import com.taxedge.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gst")
public class GstController {

    private final GstService service;
    private final UserService userService;

    public GstController(GstService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    private Long uid(UserDetails p) { return userService.getByMobile(p.getUsername()).getId(); }
    private boolean isAdmin(UserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CA"));
    }

    @PostMapping
    public ApiResponse<GstReturn> create(@AuthenticationPrincipal UserDetails p,
                                         @Valid @RequestBody GstReturnRequest req) {
        return ApiResponse.ok("Created", service.create(uid(p), req));
    }

    @GetMapping
    public ApiResponse<List<GstReturn>> mine(@AuthenticationPrincipal UserDetails p) {
        return ApiResponse.ok(service.listMine(uid(p)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','CA')")
    public ApiResponse<List<GstReturn>> all() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<GstReturn> get(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) {
        return ApiResponse.ok(service.get(id, uid(p), isAdmin(p)));
    }

    @PutMapping("/{id}")
    public ApiResponse<GstReturn> update(@AuthenticationPrincipal UserDetails p, @PathVariable Long id,
                                         @Valid @RequestBody GstReturnRequest req) {
        return ApiResponse.ok(service.update(id, uid(p), isAdmin(p), req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) {
        service.delete(id, uid(p), isAdmin(p));
        return ApiResponse.ok("Deleted", "OK");
    }
}
