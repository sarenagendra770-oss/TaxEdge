package com.taxedge.modules.document.controller;

import com.taxedge.core.common.ApiResponse;
import com.taxedge.modules.document.dto.DocumentDTO;
import com.taxedge.modules.document.entity.Document;
import com.taxedge.modules.document.service.DocumentService;
import com.taxedge.modules.user.service.UserService;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService service;
    private final UserService userService;

    public DocumentController(DocumentService service, UserService userService) {
        this.service = service; this.userService = userService;
    }
    private Long uid(UserDetails p) { return userService.getByMobile(p.getUsername()).getId(); }
    private boolean isAdmin(UserDetails p) {
        return p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CA"));
    }

    private DocumentDTO toDto(Document d) {
        return new DocumentDTO(d.getId(), d.getOriginalName(), d.getContentType(), d.getSize(), d.getCategory(), "/documents/" + d.getId() + "/download");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentDTO> upload(@AuthenticationPrincipal UserDetails p,
                                           @RequestPart("file") MultipartFile file,
                                           @RequestParam(value = "category", required = false) String category,
                                           @RequestParam(value = "remarks", required = false) String remarks) {
        return ApiResponse.ok("Uploaded", toDto(service.upload(uid(p), file, category, remarks)));
    }

    @GetMapping
    public ApiResponse<List<DocumentDTO>> mine(@AuthenticationPrincipal UserDetails p) {
        return ApiResponse.ok(service.listMine(uid(p)).stream().map(this::toDto).toList());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','CA')")
    public ApiResponse<List<DocumentDTO>> all() {
        return ApiResponse.ok(service.listAll().stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentDTO> get(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) {
        return ApiResponse.ok(toDto(service.get(id, uid(p), isAdmin(p))));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<PathResource> download(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) {
        Document d = service.get(id, uid(p), isAdmin(p));
        PathResource res = new PathResource(service.resolvePath(d));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + d.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(d.getContentType()))
                .body(res);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@AuthenticationPrincipal UserDetails p, @PathVariable Long id) {
        service.delete(id, uid(p), isAdmin(p));
        return ApiResponse.ok("Deleted", "OK");
    }
}
