package com.example.lumoo.domain.order;
import com.example.lumoo.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByOrderDateDesc(User user);
    List<Order> findByStatusOrderByOrderDateDesc(String status);
    /** customerName is NULL for most orders, and NULL <> 'ANONYMISED' is never true in SQL — hence the explicit IS NULL. */
    @Query("SELECT o FROM Order o WHERE o.createdAt < :cutoff AND (o.customerName IS NULL OR o.customerName <> 'ANONYMISED')")
    List<Order> findOrdersToAnonymise(@Param("cutoff") LocalDateTime cutoff);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.user = :user ORDER BY o.orderDate DESC")
    List<Order> findByUserWithItems(@Param("user") User user);
    @Query(value = "SELECT DISTINCT o FROM Order o JOIN o.items i WHERE o.user = :user",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o WHERE o.user = :user")
    Page<Order> findByUserPaged(@Param("user") User user, Pageable pageable);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    List<Order> findVendorOrdersWithItems(@Param("vendorId") Long vendorId);
    // Revenue = money actually received and kept: paid or later, minus refunds; never failed/cancelled/awaiting orders.
    @Query("SELECT COALESCE(SUM(o.totalAmount - o.refundedAmount), 0) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    double sumTotalRevenue();
    // EXISTS instead of JOIN: a join counted the order total once per item.
    @Query("SELECT COALESCE(SUM(o.totalAmount - o.refundedAmount), 0) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND EXISTS (SELECT 1 FROM OrderItem i WHERE i.order = o AND i.product.vendor.id = :vendorId)")
    double sumVendorRevenue(@Param("vendorId") Long vendorId);
    @Query("SELECT COALESCE(SUM(o.totalAmount - o.refundedAmount), 0) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND EXISTS (SELECT 1 FROM OrderItem i WHERE i.order = o AND i.product.vendor.id = :vendorId) AND MONTH(o.createdAt) = MONTH(CURRENT_DATE) AND YEAR(o.createdAt) = YEAR(CURRENT_DATE)")
    double sumVendorMonthlySales(@Param("vendorId") Long vendorId);
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    long countVendorOrders(@Param("vendorId") Long vendorId);
    @Query(value = "SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    Page<Order> findVendorOrdersPaged(@Param("vendorId") Long vendorId, Pageable pageable);
    @Query("SELECT COALESCE(SUM(o.adminCommission), 0) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    double sumTotalCommission();
    Optional<Order> findByPayoutTransferId(String payoutTransferId);
    List<Order> findByModempayPaymentId(String modempayPaymentId);
    List<Order> findByUserAndStatus(User user, String status);
    @Query("SELECT o FROM Order o WHERE o.status = 'AWAITING_PAYMENT' AND o.modempayPaymentId IS NOT NULL AND o.orderDate < :cutoff")
    List<Order> findStaleAwaitingPayment(@Param("cutoff") LocalDateTime cutoff);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.status = 'AWAITING_PROOF' AND o.orderDate < :cutoff")
    List<Order> findStaleAwaitingProof(@Param("cutoff") LocalDateTime cutoff);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.status = 'DELIVERED' AND o.payoutStatus = 'FAILED'")
    List<Order> findDeliveredWithFailedPayout();
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.status = 'SHIPPED' AND COALESCE(o.shippedAt, o.orderDate) < :cutoff")
    List<Order> findStaleShipped(@Param("cutoff") LocalDateTime cutoff);
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") String status);
    @Query("SELECT COALESCE(SUM(o.totalAmount - o.refundedAmount), 0) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND o.orderDate >= :since")
    double sumRevenueSince(@Param("since") LocalDateTime since);
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.payoutStatus = 'HELD' AND o.deliveredAt <= :cutoff")
    List<Order> findHeldPayoutsReadyForRelease(@Param("cutoff") LocalDateTime cutoff);
    @Query(value = "SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId AND o.status = :status",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId AND o.status = :status")
    Page<Order> findVendorOrdersPagedByStatus(@Param("vendorId") Long vendorId, @Param("status") String status, Pageable pageable);
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId AND o.status = :status")
    long countVendorOrdersByStatus(@Param("vendorId") Long vendorId, @Param("status") String status);
    /** Refund debt already recovered from this vendor's payouts (only payouts that went out, or were fully offset). */
    @Query("SELECT COALESCE(SUM(o.payoutDeduction), 0) FROM Order o WHERE o.payoutStatus IN ('PENDING', 'PAID', 'OFFSET') AND EXISTS (SELECT 1 FROM OrderItem i WHERE i.order = o AND i.product.vendor.id = :vendorId)")
    double sumPayoutDeductions(@Param("vendorId") Long vendorId);
}
