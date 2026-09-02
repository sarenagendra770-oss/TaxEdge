package com.taxedge.modules.user.controller;

import com.taxedge.core.common.ApiResponse;
import com.taxedge.modules.user.dto.UpdateUserRequest;
import com.taxedge.modules.user.dto.UserDTO;
import com.taxedge.modules.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ApiResponse<UserDTO> me(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(UserDTO.from(service.getByMobile(principal.getUsername())));
    }

    @PutMapping("/me")
    public ApiResponse<UserDTO> updateMe(@AuthenticationPrincipal UserDetails principal,
                                         @RequestBody UpdateUserRequest req) {
        return ApiResponse.ok(service.update(principal.getUsername(), req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CA')")
    public ApiResponse<List<UserDTO>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CA')")
    public ApiResponse<UserDTO> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> disable(@PathVariable Long id) {
        service.disable(id);
        return ApiResponse.ok("disabled", "User disabled");
    }
}
