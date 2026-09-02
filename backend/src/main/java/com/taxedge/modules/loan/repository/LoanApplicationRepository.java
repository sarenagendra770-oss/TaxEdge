package com.taxedge.modules.loan.repository;

import com.taxedge.modules.loan.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
}
