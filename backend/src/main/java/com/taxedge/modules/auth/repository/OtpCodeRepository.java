package com.taxedge.modules.auth.repository;

import com.taxedge.modules.auth.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findFirstByMobileAndConsumedFalseOrderByCreatedAtDesc(String mobile);
}
