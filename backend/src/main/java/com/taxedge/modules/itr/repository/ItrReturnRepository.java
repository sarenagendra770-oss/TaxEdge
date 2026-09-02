package com.taxedge.modules.itr.repository;

import com.taxedge.modules.itr.entity.ItrReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItrReturnRepository extends JpaRepository<ItrReturn, Long> {
    List<ItrReturn> findByUserIdOrderByCreatedAtDesc(Long userId);
}
