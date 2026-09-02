package com.taxedge.modules.gst.repository;

import com.taxedge.modules.gst.entity.GstReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GstReturnRepository extends JpaRepository<GstReturn, Long> {
    List<GstReturn> findByUserIdOrderByCreatedAtDesc(Long userId);
}
