package com.example.lumoo.domain.order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface RefundRepository extends JpaRepository<Refund, Long> {
    @Query("SELECT r FROM Refund r JOIN FETCH r.order o LEFT JOIN FETCH o.user WHERE r.status = :status ORDER BY r.dueBy ASC, r.id ASC")
    List<Refund> findByStatusWithOrder(@Param("status") Refund.Status status);
    @Query("SELECT r FROM Refund r WHERE r.order.id = :orderId ORDER BY r.createdAt ASC")
    List<Refund> findByOrderId(@Param("orderId") Long orderId);
    long countByStatus(Refund.Status status);
    @Query("SELECT COALESCE(SUM(r.vendorDebt), 0) FROM Refund r WHERE r.vendor.id = :vendorId")
    double sumVendorDebt(@Param("vendorId") Long vendorId);
}
