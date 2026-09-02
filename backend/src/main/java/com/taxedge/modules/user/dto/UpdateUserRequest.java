package com.taxedge.modules.user.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private String address;
    private String avatarUrl;
}
