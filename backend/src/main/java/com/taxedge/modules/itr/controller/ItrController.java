package com.taxedge.modules.itr.controller;

import com.taxedge.core.common.ApiResponse;
import com.taxedge.modules.itr.dto.ItrReturnRequest;
import com.taxedge.modules.itr.entity.ItrReturn;
import com.taxedge.modules.itr.service.ItrService;
import com.taxedge.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/itr")
public class ItrController {

    private final ItrService service;
    private final UserService userService;

    public ItrController(ItrService service, UserService userService) {
        this.service = service; this.userService = userService;
    }

    private Long uid(UserDetails p) { return userService.getByMobile(p.getUsername()).getId(); }
    private boolean isAdmin(UserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CA"));
    }

    @PostMapping
    public ApiResponse<ItrReturn> create(@AuthenticationPrincipal UserDetails p, @Valid @RequestBody ItrReturnRequest r) {
        return ApiResponse.ok("Created", service.create(uid(p), r));
    }
    @GetMapping
    public ApiResponse<List<ItrReturn>> mine(@AuthenticationPrincipal UserDetails p) { return ApiResponse.ok(service.listMine(uid(p))); }
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','CA')")
    public ApiResponse<List<ItrReturn>> all() { return ApiResponse.ok(service.listAll()); }
    @GetMapping("/{id}")
    public ApiResponse<ItrReturn> get(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) {
        return ApiResponse.ok(service.get(id, uid(p), isAdmin(p)));
    }
    @PutMapping("/{id}")
    public ApiResponse<ItrReturn> update(@AuthenticationPrincipal UserDetails p, @PathVariable Long id, @Valid @RequestBody ItrReturnRequest r) {
        return ApiResponse.ok(service.update(id, uid(p), isAdmin(p), r));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) {
        service.delete(id, uid(p), isAdmin(p)); return ApiResponse.ok("Deleted", "OK");
    }
}
