package com.example.lumoo.domain.shipping;

import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.email.EmailTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/dispatch")
public class AdminDispatchController {

    private static final Logger log = LoggerFactory.getLogger(AdminDispatchController.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private RiderService riderService;
    @Autowired private EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping({"", "/"})
    public String dispatch(Model model) {
        List<Order> readyOrders = orderRepository.findByStatusOrderByOrderDateDesc("PAID");
        model.addAttribute("orders", readyOrders);
        model.addAttribute("riders", riderService.getActive());
        model.addAttribute("inTransit", orderRepository.findByStatusOrderByOrderDateDesc("SHIPPED"));
        return "admin/dispatch";
    }

    @PostMapping("/assign")
    public String assign(@RequestParam Long orderId,
                         @RequestParam Long riderId,
                         @RequestParam(required = false) String trackingNumber,
                         @RequestParam(required = false) String estimatedDeliveryDate,
                         RedirectAttributes ra) {

        Order order = orderRepository.findById(orderId).orElse(null);
        Rider rider = riderService.findById(riderId).orElse(null);

        if (order == null || rider == null) {
            ra.addFlashAttribute("flashMsg", "Order or rider not found.");
            ra.addFlashAttribute("flashType", "red");
            return "redirect:/admin/dispatch";
        }

        // Assign rider to order
        order.setRiderId(rider.getId());
        order.setRiderName(rider.getName());
        order.setRiderPhone(rider.getPhone());
        order.setCourierName(rider.getName() + " (" + rider.getVehicleType().name() + ")");

        // Set tracking
        String tn = (trackingNumber != null && !trackingNumber.isBlank())
                ? trackingNumber.trim()
                : "LMO-" + orderId + "-" + rider.getId();
        order.setTrackingNumber(tn);
        order.setShippedAt(java.time.LocalDateTime.now());
        order.setStatus("SHIPPED");

        if (estimatedDeliveryDate != null && !estimatedDeliveryDate.isBlank()) {
            try { order.setEstimatedDeliveryDate(LocalDate.parse(estimatedDeliveryDate)); }
            catch (Exception ignored) {}
        }

        orderRepository.save(order);
        riderService.incrementDeliveries(riderId);

        // Email buyer
        emailService.sendEmail(
                order.getUser().getEmail(),
                "Your order #LMO-" + orderId + " is on its way!",
                EmailTemplates.orderShipped(order.getUser().getUsername(),
                        String.valueOf(orderId), tn, null));

        log.info("[Dispatch] Order #{} assigned to rider {} ({})", orderId, rider.getName(), rider.getPhone());

        ra.addFlashAttribute("flashMsg", "Order #LMO-" + orderId + " assigned to " + rider.getName() + ".");
        ra.addFlashAttribute("flashType", "green");
        ra.addFlashAttribute("whatsappLink", buildWhatsappMessage(order, rider, tn));
        return "redirect:/admin/dispatch";
    }

    private String buildWhatsappMessage(Order order, Rider rider, String trackingNumber) {
        String items = "";
        if (order.getItems() != null) {
            items = order.getItems().stream()
                    .map(i -> "• " + i.getProductName() + " x" + i.getQuantity())
                    .reduce("", (a, b) -> a + "\n" + b);
        }
        String msg = "🚗 *LUMOO Delivery*\n\n" +
                "Order: *#LMO-" + order.getId() + "*\n" +
                "Tracking: *" + trackingNumber + "*\n" +
                "━━━━━━━━━━\n" +
                "📦 Items:" + items + "\n\n" +
                "📍 Deliver to:\n" + order.getAddress() + "\n\n" +
                "👤 Buyer: " + order.getUser().getUsername() + "\n" +
                "💰 Amount: GMD " + String.format("%.2f", order.getTotalAmount()) + "\n" +
                "💳 Payment: " + order.getPaymentMethod() + "\n" +
                "━━━━━━━━━━\n" +
                "Track: " + baseUrl + "/track";
        return rider.whatsappLink(msg);
    }
}
