package com.taxedge.modules.insurance.repository;

import com.taxedge.modules.insurance.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByUserIdOrderByCreatedAtDesc(Long userId);
}
