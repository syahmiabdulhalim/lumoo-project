package com.example.lumoo.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OrderReportRepository extends JpaRepository<OrderReport, Long> {
    List<OrderReport> findByResolvedFalseOrderByCreatedAtDesc();
    boolean existsByOrderIdAndResolvedFalse(Long orderId);
    long countByResolvedFalse();
    @Query("SELECT r FROM OrderReport r LEFT JOIN FETCH r.order LEFT JOIN FETCH r.reporter WHERE r.resolved = false ORDER BY r.createdAt DESC")
    List<OrderReport> findOpenWithDetails();
}
