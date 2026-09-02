package com.taxedge.modules.user.dto;

import com.taxedge.modules.user.entity.Role;
import com.taxedge.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String mobile;
    private String fullName;
    private String email;
    private String customerType;
    private LocalDate dob;
    private String pan;
    private String aadhaar;
    private String address;
    private String avatarUrl;
    private Role role;
    private boolean enabled;
    private boolean profileComplete;

    public static UserDTO from(User u) {
        return UserDTO.builder()
                .id(u.getId())
                .mobile(u.getMobile())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .customerType(u.getCustomerType())
                .dob(u.getDob())
                .pan(u.getPan())
                .aadhaar(u.getAadhaar())
                .address(u.getAddress())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole())
                .enabled(u.isEnabled())
                .profileComplete(u.isProfileComplete())
                .build();
    }
}
