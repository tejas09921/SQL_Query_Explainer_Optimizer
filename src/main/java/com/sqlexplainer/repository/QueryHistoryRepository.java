package com.sqlexplainer.repository;

import com.sqlexplainer.model.QueryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    Page<QueryHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
