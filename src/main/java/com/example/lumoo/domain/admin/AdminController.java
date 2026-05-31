package com.example.lumoo.domain.admin;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.admin.SiteSettings;
import com.example.lumoo.domain.vendor.VendorApplication;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.product.ReviewService;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.order.CartService;
import com.example.lumoo.domain.payment.ModemPayService;
import com.example.lumoo.domain.user.UserService;
import com.example.lumoo.domain.user.CustomUserDetailsService;
import com.example.lumoo.domain.vendor.VendorApplicationService;
import com.example.lumoo.domain.blog.BlogService;
import com.example.lumoo.domain.admin.SiteSettingsService;
import com.example.lumoo.domain.inquiry.InquiryService;
import com.example.lumoo.domain.subscriber.SubscriberService;
import com.example.lumoo.domain.pdpp.AuditService;
import com.example.lumoo.domain.pdpp.CustomerRightsService;
import com.example.lumoo.domain.pdpp.DataBreachService;
import com.example.lumoo.domain.pdpp.DataRetentionService;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.domain.pdpp.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    @Autowired private OrderService orderService;
    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private InquiryService inquiryService;
    @Autowired private VendorApplicationService vendorApplicationService;
    @Autowired private SiteSettingsService siteSettingsService;
    @Autowired private SubscriberService subscriberService;
    @Autowired private AuditService auditService;
    @Autowired private com.example.lumoo.domain.pdpp.AuditLogRepository auditLogRepository;
    @Autowired private com.example.lumoo.infrastructure.email.EmailService emailService;
    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(defaultValue = "0") int page, Model model) {
        var logsPage = auditLogRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(page, 50));
        model.addAttribute("logs", logsPage.getContent());
        model.addAttribute("currentPage", logsPage.getNumber());
        model.addAttribute("totalPages", logsPage.getTotalPages());
        model.addAttribute("totalLogs", logsPage.getTotalElements());
        return "admin/audit-logs";
    }
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalOrders", orderService.countAll());
        model.addAttribute("totalRevenue", orderService.sumTotalRevenue());
        model.addAttribute("totalCommission", orderService.sumTotalCommission());
        model.addAttribute("totalUsers", userService.countAll());
        return "admin/dashboard";
    }
    private static final int ORDERS_PAGE_SIZE = 50;
    @GetMapping("/sections/orders")
    public String sectionOrders(Model model,
                                @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page) {
        var ordersPage = orderService.getPage(page, ORDERS_PAGE_SIZE);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("ordersCurrentPage", ordersPage.getNumber());
        model.addAttribute("ordersTotalPages", ordersPage.getTotalPages());
        model.addAttribute("proofOrders", ordersPage.getContent().stream()
                .filter(o -> "PROOF_UPLOADED".equals(o.getStatus())).toList());
        return "admin/sections/orders :: content";
    }
    @GetMapping("/sections/moderation")
    public String sectionModeration(Model model) {
        model.addAttribute("pendingProducts", productService.getPendingApproval());
        model.addAttribute("vendorApplications", vendorApplicationService.getPending());
        model.addAttribute("pendingImages", productService.getPendingImageApproval());
        return "admin/sections/moderation :: content";
    }
    private static final int INVENTORY_PAGE_SIZE = 25;
    private static final int USERS_PAGE_SIZE = 25;
    @GetMapping("/sections/inventory")
    public String sectionInventory(Model model,
                                   @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page) {
        var productsPage = productService.getAllPage(page, INVENTORY_PAGE_SIZE);
        model.addAttribute("products", productsPage.getContent());
        model.addAttribute("inventoryCurrentPage", productsPage.getNumber());
        model.addAttribute("inventoryTotalPages", productsPage.getTotalPages());
        return "admin/sections/inventory :: content";
    }
    @GetMapping("/sections/users")
    public String sectionUsers(Model model,
                               @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page) {
        var usersPage = userService.getPage(page, USERS_PAGE_SIZE);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("usersCurrentPage", usersPage.getNumber());
        model.addAttribute("usersTotalPages", usersPage.getTotalPages());
        model.addAttribute("inquiries", inquiryService.getAll());
        return "admin/sections/users :: content";
    }
    @GetMapping("/approve-product/{id}")
    public String approveProduct(@PathVariable Long id, HttpServletRequest req) {
        productService.approve(id);
        auditService.log("PRODUCT_APPROVED", "Product", String.valueOf(id), req);
        log.info("[Admin] Product #{} approved by {}", id, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard?approved#moderation";
    }
    @GetMapping("/reject-product/{id}")
    public String rejectProduct(@PathVariable Long id, HttpServletRequest req) {
        productService.delete(id);
        auditService.log("PRODUCT_REJECTED", "Product", String.valueOf(id), req);
        log.info("[Admin] Product #{} rejected/deleted by {}", id, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard?rejected#moderation";
    }
    @GetMapping("/delete-product/{id}")
    public String adminDeleteProduct(@PathVariable Long id, HttpServletRequest req) {
        productService.delete(id);
        auditService.log("PRODUCT_DELETED", "Product", String.valueOf(id), req);
        log.info("[Admin] Product #{} deleted by {}", id, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard?deleted_product#inventory";
    }
    @GetMapping("/delete-user/{id}")
    public String adminDeleteUser(@PathVariable Long id, HttpServletRequest req) {
        userService.delete(id);
        auditService.log("USER_DELETED", "User", String.valueOf(id), req);
        log.warn("[Admin] User #{} deleted by {}", id, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard?deleted_user#users";
    }
    @PostMapping("/verify-user/{id}")
    public String verifyUser(@PathVariable Long id, RedirectAttributes ra, HttpServletRequest req) {
        boolean done = userService.verifyUser(id);
        if (done) auditService.log("USER_VERIFIED", "User", String.valueOf(id), req);
        ra.addFlashAttribute("flashMsg", done ? "User verified successfully." : "User is already verified.");
        ra.addFlashAttribute("flashType", done ? "green" : "blue");
        return "redirect:/admin/dashboard#users";
    }
    @PostMapping("/upgrade-vendor/{id}")
    public String upgradeToVendor(@PathVariable Long id, RedirectAttributes ra, HttpServletRequest req) {
        boolean done = userService.upgradeToVendor(id);
        if (done) auditService.log("USER_UPGRADED_TO_VENDOR", "User", String.valueOf(id), req);
        ra.addFlashAttribute("flashMsg", done ? "User upgraded to Vendor." : "Could not upgrade (already Vendor/Admin).");
        ra.addFlashAttribute("flashType", done ? "green" : "blue");
        return "redirect:/admin/dashboard#users";
    }
    @PostMapping("/update-order-status")
    public String updateOrderStatus(@RequestParam Long orderId, @RequestParam String status, HttpServletRequest req) {
        orderService.updateStatus(orderId, status);
        auditService.log("ORDER_STATUS_UPDATED", "Order", String.valueOf(orderId), null, java.util.Map.of("status", status), req);
        log.info("[Admin] Order #{} status set to {} by {}", orderId, status, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard#orders";
    }
    @GetMapping("/delete-order/{id}")
    public String deleteOrder(@PathVariable Long id, HttpServletRequest req) {
        orderService.delete(id);
        auditService.log("ORDER_DELETED", "Order", String.valueOf(id), req);
        log.warn("[Admin] Order #{} deleted by {}", id, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard#orders";
    }
    @GetMapping("/delete-inquiry/{id}")
    public String deleteInquiry(@PathVariable Long id, HttpServletRequest req) {
        inquiryService.delete(id);
        auditService.log("INQUIRY_DELETED", "Inquiry", String.valueOf(id), req);
        return "redirect:/admin/dashboard?deleted_inquiry#inquiries";
    }
    @PostMapping("/order/{id}/resolve-return")
    public String resolveReturn(@PathVariable Long id, HttpServletRequest req) {
        orderService.resolveReturn(id);
        auditService.log("RETURN_RESOLVED", "Order", String.valueOf(id), req);
        return "redirect:/admin/dashboard?return_resolved#orders";
    }
    @PostMapping("/order/{id}/verify-payment")
    public String verifyPayment(@PathVariable Long id, HttpServletRequest req) {
        orderService.verifyPayment(id);
        auditService.log("PAYMENT_VERIFIED", "Order", String.valueOf(id), req);
        log.info("[Admin] Payment verified for order #{} by {}", id, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard?payment_verified#proof-review";
    }
    @GetMapping("/approve-image/{id}")
    public String approveImage(@PathVariable Long id, HttpServletRequest req) {
        productService.approveImage(id);
        auditService.log("IMAGE_APPROVED", "Product", String.valueOf(id), req);
        return "redirect:/admin/dashboard?image_approved#images";
    }
    @GetMapping("/reject-image/{id}")
    public String rejectImage(@PathVariable Long id, HttpServletRequest req) {
        productService.rejectImage(id);
        auditService.log("IMAGE_REJECTED", "Product", String.valueOf(id), req);
        return "redirect:/admin/dashboard?image_rejected#images";
    }
    @GetMapping("/application/{id}")
    public String applicationDetail(@PathVariable Long id, Model model) {
        VendorApplication app = vendorApplicationService.findById(id).orElse(null);
        if (app == null) return "redirect:/admin/dashboard#vendor-applications";
        model.addAttribute("app", app);
        return "admin/application-detail";
    }
    @GetMapping("/approve-vendor/{id}")
    public String approveVendor(@PathVariable Long id, HttpServletRequest req) {
        vendorApplicationService.approve(id);
        auditService.log("VENDOR_APPLICATION_APPROVED", "VendorApplication", String.valueOf(id), req);
        log.info("[Admin] Vendor application #{} approved by {}", id, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard?vendor_approved";
    }
    @PostMapping("/reject-vendor/{id}")
    public String rejectVendor(@PathVariable Long id,
                               @RequestParam(required = false) String note,
                               HttpServletRequest req) {
        vendorApplicationService.reject(id, note);
        auditService.log("VENDOR_APPLICATION_REJECTED", "VendorApplication", String.valueOf(id), req);
        log.info("[Admin] Vendor application #{} rejected by {}", id, req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "?");
        return "redirect:/admin/dashboard?vendor_rejected";
    }
    @GetMapping("/settings")
    public String settings(Model model) {
        var subs = subscriberService.getAll();
        model.addAttribute("settings", siteSettingsService.get());
        model.addAttribute("subscribers", subs);
        model.addAttribute("activeSubscriberCount", subs.stream().filter(s -> s.isActive()).count());
        return "admin/settings";
    }
    @PostMapping("/subscriber/{id}/delete")
    public String deleteSubscriber(@PathVariable Long id) {
        subscriberService.delete(id);
        return "redirect:/admin/settings?tab=subscribers";
    }
    @PostMapping("/newsletter/send")
    public String sendNewsletter(@RequestParam String subject,
                                 @RequestParam String htmlBody,
                                 RedirectAttributes ra) {
        if (subject.isBlank() || htmlBody.isBlank()) {
            ra.addFlashAttribute("flashMsg", "Subject and body are required.");
            ra.addFlashAttribute("flashType", "red");
            return "redirect:/admin/settings?tab=newsletter";
        }
        var subscribers = subscriberService.getActive();
        for (var sub : subscribers) {
            emailService.sendEmail(sub.getEmail(), subject, htmlBody);
        }
        log.info("[Admin] Newsletter sent to {} subscribers", subscribers.size());
        ra.addFlashAttribute("flashMsg", "Newsletter queued for " + subscribers.size() + " subscribers.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/settings?tab=newsletter";
    }
    @PostMapping("/settings")
    public String saveSettings(@ModelAttribute SiteSettings settings, RedirectAttributes ra) {
        siteSettingsService.save(settings);
        ra.addFlashAttribute("flashMsg", "Settings saved successfully.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/settings";
    }
}
