package com.taxedge.modules.user.entity;

import com.taxedge.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "mobile"),
        @UniqueConstraint(columnNames = "email")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 15)
    private String mobile;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(length = 40)
    private String customerType;

    private LocalDate dob;

    @Column(length = 10)
    private String pan;

    @Column(length = 20)
    private String aadhaar;

    @Column(length = 500)
    private String address;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** true only after profile is filled via /auth/register. */
    @Column(nullable = false)
    @Builder.Default
    private boolean profileComplete = false;
}
