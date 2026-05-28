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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private OrderService orderService;
    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private InquiryService inquiryService;
    @Autowired private VendorApplicationService vendorApplicationService;
    @Autowired private SiteSettingsService siteSettingsService;
    @Autowired private SubscriberService subscriberService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        List<Order> orders = orderService.getAll();
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("totalRevenue", orders.stream().mapToDouble(Order::getTotalAmount).sum());
        model.addAttribute("totalCommission", orders.stream().mapToDouble(Order::getAdminCommission).sum());
        model.addAttribute("products", productService.getAll());
        model.addAttribute("users", userService.getAll());
        model.addAttribute("orders", orders);
        model.addAttribute("inquiries", inquiryService.getAll());
        model.addAttribute("pendingProducts", productService.getPendingApproval());
        model.addAttribute("vendorApplications", vendorApplicationService.getPending());
        model.addAttribute("pendingImages", productService.getPendingImageApproval());
        model.addAttribute("proofOrders", orders.stream()
                .filter(o -> "PROOF_UPLOADED".equals(o.getStatus())).toList());
        return "admin/dashboard";
    }

    @GetMapping("/approve-product/{id}")
    public String approveProduct(@PathVariable Long id) {
        productService.approve(id);
        return "redirect:/admin/dashboard?approved#moderation";
    }

    @GetMapping("/reject-product/{id}")
    public String rejectProduct(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/dashboard?rejected#moderation";
    }

    @GetMapping("/delete-product/{id}")
    public String adminDeleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/dashboard?deleted_product#inventory";
    }

    @GetMapping("/delete-user/{id}")
    public String adminDeleteUser(@PathVariable Long id) {
        userService.delete(id);
        return "redirect:/admin/dashboard?deleted_user#users";
    }

    @PostMapping("/verify-user/{id}")
    public String verifyUser(@PathVariable Long id, RedirectAttributes ra) {
        boolean done = userService.verifyUser(id);
        ra.addFlashAttribute("flashMsg", done ? "User verified successfully." : "User is already verified.");
        ra.addFlashAttribute("flashType", done ? "green" : "blue");
        return "redirect:/admin/dashboard#users";
    }

    @PostMapping("/upgrade-vendor/{id}")
    public String upgradeToVendor(@PathVariable Long id, RedirectAttributes ra) {
        boolean done = userService.upgradeToVendor(id);
        ra.addFlashAttribute("flashMsg", done ? "User upgraded to Vendor." : "Could not upgrade (already Vendor/Admin).");
        ra.addFlashAttribute("flashType", done ? "green" : "blue");
        return "redirect:/admin/dashboard#users";
    }

    @PostMapping("/update-order-status")
    public String updateOrderStatus(@RequestParam Long orderId, @RequestParam String status) {
        orderService.updateStatus(orderId, status);
        return "redirect:/admin/dashboard#orders";
    }

    @GetMapping("/delete-order/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
        return "redirect:/admin/dashboard#orders";
    }

    @GetMapping("/delete-inquiry/{id}")
    public String deleteInquiry(@PathVariable Long id) {
        inquiryService.delete(id);
        return "redirect:/admin/dashboard?deleted_inquiry#inquiries";
    }

    @PostMapping("/order/{id}/resolve-return")
    public String resolveReturn(@PathVariable Long id) {
        orderService.resolveReturn(id);
        return "redirect:/admin/dashboard?return_resolved#orders";
    }

    @PostMapping("/order/{id}/verify-payment")
    public String verifyPayment(@PathVariable Long id) {
        orderService.verifyPayment(id);
        return "redirect:/admin/dashboard?payment_verified#proof-review";
    }

    @GetMapping("/approve-image/{id}")
    public String approveImage(@PathVariable Long id) {
        productService.approveImage(id);
        return "redirect:/admin/dashboard?image_approved#images";
    }

    @GetMapping("/reject-image/{id}")
    public String rejectImage(@PathVariable Long id) {
        productService.rejectImage(id);
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
    public String approveVendor(@PathVariable Long id) {
        vendorApplicationService.approve(id);
        return "redirect:/admin/dashboard?vendor_approved";
    }

    @PostMapping("/reject-vendor/{id}")
    public String rejectVendor(@PathVariable Long id,
                               @RequestParam(required = false) String note) {
        vendorApplicationService.reject(id, note);
        return "redirect:/admin/dashboard?vendor_rejected";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("settings", siteSettingsService.get());
        model.addAttribute("subscribers", subscriberService.getAll());
        return "admin/settings";
    }

    @PostMapping("/subscriber/{id}/delete")
    public String deleteSubscriber(@PathVariable Long id) {
        subscriberService.delete(id);
        return "redirect:/admin/settings?tab=subscribers";
    }

    @PostMapping("/settings")
    public String saveSettings(@ModelAttribute SiteSettings settings, RedirectAttributes ra) {
        siteSettingsService.save(settings);
        ra.addFlashAttribute("flashMsg", "Settings saved successfully.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/settings";
    }
}
