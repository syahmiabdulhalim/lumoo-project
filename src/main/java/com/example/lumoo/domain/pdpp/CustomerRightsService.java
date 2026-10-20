package com.example.lumoo.domain.pdpp;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class CustomerRightsService {
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ErasureRequestRepository erasureRequestRepository;
    @Autowired private DataAccessRequestRepository dataAccessRequestRepository;
    @Autowired private AuditService auditService;
    @Transactional
    public Map<String, Object> processErasureRequest(String email, HttpServletRequest request) {
        String refId = UUID.randomUUID().toString();
        ErasureRequest er = new ErasureRequest();
        er.setEmail(email);
        er.setReferenceId(refId);
        er.setStatus(ErasureRequest.Status.PROCESSING);
        erasureRequestRepository.save(er);
        Optional<User> userOpt = userRepository.findByEmail(email);
        int ordersAnonymised = 0;
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(user);
            for (Order order : orders) {
                if (!"ANONYMISED".equals(order.getCustomerName())) {
                    order.setCustomerName("ANONYMISED");
                    order.setAddress(null);
                    orderRepository.save(order);
                    ordersAnonymised++;
                }
            }
            String anonSuffix = user.getId().toString();
            user.setEmail("anonymised-" + anonSuffix + "@deleted.invalid");
            user.setFullName("ANONYMISED");
            user.setPhone(null);
            user.setAddress(null);
            user.setUsername("deleted-" + anonSuffix);
            userRepository.save(user);
        }
        er.setStatus(ErasureRequest.Status.COMPLETED);
        er.setProcessedAt(LocalDateTime.now());
        er.setOrdersAffected(ordersAnonymised);
        erasureRequestRepository.save(er);
        auditService.log("ERASURE_REQUEST_PROCESSED", "User", email,
                Map.of("ordersAnonymised", ordersAnonymised, "referenceId", refId),
                Map.of("status", "COMPLETED", "processedAt", LocalDateTime.now().toString()),
                request);
        return Map.of(
                "message", "Your data erasure request has been processed. " +
                           "We will retain financial records as required by Gambian law (7 years). " +
                           "All personal identifiers have been anonymised.",
                "referenceId", refId,
                "ordersAnonymised", ordersAnonymised,
                "governingLaw", "PDPP 2025 — The Gambia, s.24"
        );
    }
    @Transactional
    public Map<String, Object> processDataAccessRequest(String email, HttpServletRequest request) {
        String refId = UUID.randomUUID().toString();
        DataAccessRequest dar = new DataAccessRequest();
        dar.setEmail(email);
        dar.setReferenceId(refId);
        dataAccessRequestRepository.save(dar);
        Optional<User> userOpt = userRepository.findByEmail(email);
        List<Map<String, Object>> ordersSummary = new ArrayList<>();
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(user);
            for (Order o : orders) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("orderId", o.getId());
                entry.put("date", o.getCreatedAt());
                entry.put("amount", o.getTotalAmount());
                entry.put("status", o.getStatus());
                entry.put("paymentMethod", o.getPaymentMethod());
                ordersSummary.add(entry);
            }
        }
        dar.setFulfilledAt(LocalDateTime.now());
        dataAccessRequestRepository.save(dar);
        auditService.log("DATA_ACCESS_REQUEST", "User", email, null,
                Map.of("referenceId", refId, "ordersReturned", ordersSummary.size()), request);
        return Map.of(
                "email", email,
                "ordersCount", ordersSummary.size(),
                "orders", ordersSummary,
                "referenceId", refId,
                "dataRetentionPeriod", "7 years from order date (financial records)",
                "governingLaw", "PDPP 2025 — The Gambia, s.23"
        );
    }
}
