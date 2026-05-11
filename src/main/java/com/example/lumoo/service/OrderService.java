package com.example.lumoo.service;

import com.example.lumoo.model.*;
import com.example.lumoo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private CartRepository cartRepository;

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    public List<Order> getVendorOrders(Long vendorId) {
        return orderRepository.findOrdersByVendorId(vendorId);
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order placeOrder(User user, String address, String paymentMethod, List<CartItem> cartItems) {
        double total = cartItems.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        double commission = total * 0.10;
        double vendorEarnings = total - commission;

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address.trim());
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        order.setStatus(paymentMethod.equals("COD") ? "PENDING" : "PAID");
        order.setTotalAmount(total);
        order.setAdminCommission(commission);
        order.setVendorEarnings(vendorEarnings);

        Order saved = orderRepository.save(order);

        for (CartItem ci : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(saved);
            oi.setProductName(ci.getName());
            oi.setPrice(ci.getPrice());
            oi.setQuantity(ci.getQuantity());
            orderItemRepository.save(oi);
        }

        cartRepository.deleteAll(cartItems);
        return saved;
    }

    public enum CancelResult { CANCELLED, NOT_FOUND, UNAUTHORIZED, CANNOT_CANCEL }

    public CancelResult cancelOrder(Long id, User user) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return CancelResult.NOT_FOUND;
        if (!order.getUser().getId().equals(user.getId())) return CancelResult.UNAUTHORIZED;
        if (!order.getStatus().equals("PENDING")) return CancelResult.CANNOT_CANCEL;
        orderRepository.delete(order);
        return CancelResult.CANCELLED;
    }

    public void updateStatus(Long orderId, String status) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus(status);
            orderRepository.save(o);
        });
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}
