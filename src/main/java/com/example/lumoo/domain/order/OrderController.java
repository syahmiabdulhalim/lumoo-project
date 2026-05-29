package com.example.lumoo.domain.order;

import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.order.CartService;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.payment.ModemPayService;
import com.example.lumoo.domain.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired private OrderService orderService;
    @Autowired private CartService cartService;
    @Autowired private UserService userService;
    @Autowired private ModemPayService modemPayService;

    @Value("${app.upload.dir:/app/uploads/products}")
    private String uploadDir;

    private static final Set<String> VALID_PAYMENT_METHODS = Set.of("COD", "TRANSFER", "AFRIMONEY", "QMONEY", "MODEMPAY");
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/jpg");

    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        List<CartItem> items = cartService.getItems(user);
        if (items.isEmpty()) return "redirect:/cart";
        model.addAttribute("cart", items);
        model.addAttribute("total", cartService.getTotal(items));
        return "checkout";
    }

    @PostMapping("/order/place")
    public String placeOrder(@RequestParam String address,
                             @RequestParam String paymentMethod,
                             @RequestParam(defaultValue = "false") boolean privacyAccepted,
                             @RequestParam(defaultValue = "false") boolean termsAccepted,
                             @RequestParam(defaultValue = "false") boolean marketingConsent,
                             Principal principal) {
        if (principal == null) return "redirect:/login";
        if (address == null || address.trim().isEmpty()) return "redirect:/checkout?error=address_required";
        if (!VALID_PAYMENT_METHODS.contains(paymentMethod)) return "redirect:/checkout?error=invalid_payment";
        if (!privacyAccepted || !termsAccepted) return "redirect:/checkout?error=consent_required";

        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        List<CartItem> items = cartService.getItems(user);
        if (items.isEmpty()) return "redirect:/cart?error=empty";

        List<Order> orders;
        try {
            orders = orderService.placeOrders(user, address, paymentMethod, items, privacyAccepted, termsAccepted, marketingConsent);
        } catch (IllegalStateException e) {
            return "redirect:/cart?error=out_of_stock";
        }

        if ("COD".equals(paymentMethod)) {
            return orders.size() == 1
                    ? "redirect:/buyer/order/" + orders.get(0).getId() + "?success"
                    : "redirect:/buyer/dashboard?orders_placed";
        }

        if ("MODEMPAY".equals(paymentMethod)) {
            if (!modemPayService.isConfigured()) {
                return "redirect:/buyer/dashboard?error=payment_unavailable";
            }
            try {
                String paymentUrl = modemPayService.createPaymentIntent(orders);
                return "redirect:" + paymentUrl;
            } catch (Exception e) {
                log.error("[OrderController] ModemPay intent failed for {} orders", orders.size(), e);
                return "redirect:/buyer/dashboard?error=payment_failed";
            }
        }

        return orders.size() == 1
                ? "redirect:/checkout/success/" + orders.get(0).getId()
                : "redirect:/buyer/dashboard?orders_placed";
    }

    @GetMapping("/checkout/success/{id}")
    public String checkoutSuccess(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        Order order = orderService.findById(id).orElse(null);
        if (order == null || !order.getUser().getId().equals(user.getId()))
            return "redirect:/buyer/dashboard";
        model.addAttribute("orderId", order.getId());
        model.addAttribute("paymentMethod", order.getPaymentMethod());
        model.addAttribute("totalAmount", order.getTotalAmount());
        return "checkout-success";
    }

    @PostMapping("/order/{id}/upload-proof")
    public String uploadProof(@PathVariable Long id,
                              @RequestParam("proof") MultipartFile proof,
                              Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        Order order = orderService.findById(id).orElse(null);
        if (order == null || !order.getUser().getId().equals(user.getId()))
            return "redirect:/buyer/dashboard?error=not_found";
        if (!order.getStatus().equals("AWAITING_PROOF"))
            return "redirect:/buyer/order/" + id + "?error=already_uploaded";

        if (proof == null || proof.isEmpty())
            return "redirect:/buyer/order/" + id + "?error=no_file";

        String ct = proof.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct.toLowerCase()))
            return "redirect:/buyer/order/" + id + "?error=invalid_type";

        if (proof.getSize() > 5 * 1024 * 1024)
            return "redirect:/buyer/order/" + id + "?error=too_large";

        try {
            String original = proof.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf(".")).toLowerCase() : ".jpg";
            String filename = "proof-" + UUID.randomUUID() + ext;
            Path proofDir = Paths.get(uploadDir).getParent().resolve("proofs");
            Files.createDirectories(proofDir);
            Files.copy(proof.getInputStream(), proofDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            String proofUrl = "/uploads/proofs/" + filename;
            orderService.submitProof(id, proofUrl);
        } catch (IOException e) {
            return "redirect:/buyer/order/" + id + "?error=upload_failed";
        }

        return "redirect:/buyer/order/" + id + "?proof_uploaded";
    }
}
