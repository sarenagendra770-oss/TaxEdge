package com.taxedge.modules.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private String originalName;
    private String contentType;
    private Long size;
    private String category;
    private String downloadUrl;
}
