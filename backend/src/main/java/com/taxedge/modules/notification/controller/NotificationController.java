package com.taxedge.modules.notification.controller;

import com.taxedge.core.common.ApiResponse;
import com.taxedge.modules.notification.dto.NotificationRequest;
import com.taxedge.modules.notification.entity.Notification;
import com.taxedge.modules.notification.service.NotificationService;
import com.taxedge.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final UserService userService;
    public NotificationController(NotificationService service, UserService userService) {
        this.service = service; this.userService = userService;
    }
    private Long uid(UserDetails p) { return userService.getByMobile(p.getUsername()).getId(); }
    private boolean isAdmin(UserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CA"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CA')")
    public ApiResponse<Notification> send(@Valid @RequestBody NotificationRequest r) { return ApiResponse.ok("Sent", service.send(r)); }

    @GetMapping public ApiResponse<List<Notification>> mine(@AuthenticationPrincipal UserDetails p) { return ApiResponse.ok(service.listMine(uid(p))); }
    @GetMapping("/unread-count") public ApiResponse<Map<String,Long>> unread(@AuthenticationPrincipal UserDetails p) { return ApiResponse.ok(Map.of("count", service.unreadCount(uid(p)))); }
    @PatchMapping("/{id}/read") public ApiResponse<Notification> read(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) { return ApiResponse.ok(service.markRead(id, uid(p), isAdmin(p))); }
    @DeleteMapping("/{id}") public ApiResponse<String> delete(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) { service.delete(id, uid(p), isAdmin(p)); return ApiResponse.ok("Deleted", "OK"); }
}
