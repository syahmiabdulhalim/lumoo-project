package com.example.lumoo.domain.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/track")
public class OrderTrackingController {

    @Autowired private OrderRepository orderRepository;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter D  = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @GetMapping({"", "/"})
    public String form(Model model) {
        return "track";
    }

    @PostMapping
    public String lookup(@RequestParam String orderId,
                         @RequestParam String email,
                         Model model) {
        Long id;
        try {
            String cleaned = orderId.trim().toUpperCase().replace("LMO-", "").replace("#", "");
            id = Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Invalid order ID. Enter your order number, e.g. LMO-123.");
            return "track";
        }

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null
                || order.getUser() == null
                || !order.getUser().getEmail().equalsIgnoreCase(email.trim())) {
            model.addAttribute("error", "No order found. Please check your Order ID and email address.");
            return "track";
        }

        model.addAttribute("order", order);
        model.addAttribute("items", order.getItems());
        model.addAttribute("events", buildEvents(order));
        model.addAttribute("step", currentStep(order.getStatus()));
        return "track";
    }

    private int currentStep(String status) {
        return switch (status == null ? "" : status) {
            case "PENDING", "AWAITING_PAYMENT", "AWAITING_PROOF", "PROOF_UPLOADED" -> 1;
            case "PAID"      -> 2;
            case "SHIPPED"   -> 3;
            case "DELIVERED" -> 4;
            case "CANCELLED", "PAYMENT_FAILED", "PAYMENT_EXPIRED", "RETURN_REQUESTED", "RETURNED" -> 0;
            default          -> 1;
        };
    }

    private List<TrackingEvent> buildEvents(Order order) {
        List<TrackingEvent> events = new ArrayList<>();
        String status = order.getStatus();

        LocalDateTime placed = order.getOrderDate() != null ? order.getOrderDate()
                             : order.getCreatedAt() != null ? order.getCreatedAt()
                             : LocalDateTime.now();

        // Cancelled / failed states
        if ("CANCELLED".equals(status)) {
            events.add(new TrackingEvent(placed.format(DT), "Order Placed", "Order received and recorded.", "done"));
            events.add(new TrackingEvent("—", "Order Cancelled", "This order has been cancelled.", "cancelled"));
            return events;
        }
        if ("PAYMENT_FAILED".equals(status) || "PAYMENT_EXPIRED".equals(status)) {
            events.add(new TrackingEvent(placed.format(DT), "Order Placed", "Order received and recorded.", "done"));
            events.add(new TrackingEvent("—", "Payment Unsuccessful", "Payment could not be completed. Please contact support.", "cancelled"));
            return events;
        }
        if ("RETURN_REQUESTED".equals(status)) {
            events.add(new TrackingEvent(placed.format(DT), "Order Placed", "Order received and recorded.", "done"));
            events.add(new TrackingEvent(placed.plusHours(1).format(DT), "Payment Confirmed", "Your payment has been verified.", "done"));
            events.add(new TrackingEvent("—", "Return Requested", "Return request received. Our team will contact you shortly.", "warning"));
            return events;
        }
        if ("RETURNED".equals(status)) {
            events.add(new TrackingEvent(placed.format(DT), "Order Placed", "Order received and recorded.", "done"));
            events.add(new TrackingEvent("—", "Return Processed", "Your return has been processed.", "warning"));
            return events;
        }

        // Normal flow
        events.add(new TrackingEvent(placed.format(DT), "Order Placed", "Your order has been received and recorded in our system.", "done"));

        boolean paid     = List.of("PAID","SHIPPED","DELIVERED").contains(status);
        boolean shipped  = List.of("SHIPPED","DELIVERED").contains(status);
        boolean delivered = "DELIVERED".equals(status);

        if (paid) {
            events.add(new TrackingEvent(placed.plusMinutes(30).format(DT), "Payment Confirmed", "Payment verified. Your order is now being prepared.", "done"));
            events.add(new TrackingEvent(placed.plusHours(2).format(DT), "Order Processing", "Vendor is packing your items for dispatch.", "done"));
        } else {
            events.add(new TrackingEvent("Pending", "Payment Confirmation", "Awaiting payment verification.", "pending"));
        }

        if (shipped) {
            String shippedTime = order.getShippedAt() != null
                    ? order.getShippedAt().format(DT)
                    : placed.plusDays(1).format(DT);
            String trackInfo = (order.getTrackingNumber() != null && !order.getTrackingNumber().isBlank())
                    ? "Tracking No: " + order.getTrackingNumber()
                    : "Your order is on its way to you.";
            events.add(new TrackingEvent(shippedTime, "Dispatched", trackInfo, "done"));

            if (delivered) {
                String deliveredTime = order.getEstimatedDeliveryDate() != null
                        ? order.getEstimatedDeliveryDate().format(D)
                        : (order.getShippedAt() != null ? order.getShippedAt().plusDays(2).format(DT) : "—");
                events.add(new TrackingEvent(deliveredTime, "Delivered", "Your order has been successfully delivered. Thank you!", "done"));
            } else {
                String eta = order.getEstimatedDeliveryDate() != null
                        ? "Expected by " + order.getEstimatedDeliveryDate().format(D)
                        : "Delivery in progress.";
                events.add(new TrackingEvent("In Progress", "Out for Delivery", eta, "active"));
            }
        } else if (paid) {
            events.add(new TrackingEvent("Pending", "Dispatching Soon", "Vendor will dispatch your order shortly.", "pending"));
        }

        return events;
    }

    public record TrackingEvent(String time, String title, String description, String type) {}
}
