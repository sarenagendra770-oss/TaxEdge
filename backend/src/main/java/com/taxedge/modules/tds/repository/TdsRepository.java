package com.taxedge.modules.tds.repository;

import com.taxedge.modules.tds.entity.TdsRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TdsRepository extends JpaRepository<TdsRecord, Long> {
    List<TdsRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
