package com.example.lumoo.shared;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.Review;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.payment.WebhookEvent;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.Notification;
import com.example.lumoo.domain.vendor.VendorApplication;
import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.admin.SiteSettings;
import com.example.lumoo.domain.inquiry.Inquiry;
import com.example.lumoo.domain.subscriber.Subscriber;
import com.example.lumoo.domain.pdpp.AuditLog;
import com.example.lumoo.domain.pdpp.ErasureRequest;
import com.example.lumoo.domain.pdpp.DataAccessRequest;
import com.example.lumoo.domain.pdpp.BreachIncident;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
@Controller
public class WebController {
    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private OrderService orderService;
    @Autowired private ReviewService reviewService;
    @Autowired private VendorApplicationService vendorApplicationService;
    @Autowired private BlogService blogService;
    @Autowired private SubscriberService subscriberService;
    @Autowired private SiteSettingsService siteSettingsService;
    @Autowired private ModemPayService modemPayService;
    @GetMapping("/stores")
    public String stores(Model model) {
        List<User> vendors = productService.getVendorsWithProducts();
        model.addAttribute("vendors", vendors);
        return "stores";
    }
    @GetMapping("/store/{vendorId}")
    public String store(@PathVariable Long vendorId,
                        @RequestParam(required = false) String category,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        User vendor = userService.findById(vendorId).orElse(null);
        if (vendor == null) return "redirect:/stores";
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").descending());
        Page<Product> productPage = (category != null && !category.isEmpty())
                ? productService.getApprovedByVendorAndCategoryPaged(vendor, category, pageable)
                : productService.getApprovedByVendorPaged(vendor, pageable);
        model.addAttribute("vendor", vendor);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalProductCount", productPage.getTotalElements());
        model.addAttribute("categories", productService.getDistinctCategoriesByVendor(vendor));
        model.addAttribute("selectedCategory", category);
        return "store";
    }
    private static final int PAGE_SIZE = 12;
    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "0") int page) {
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").descending());
        Page<Product> productPage;
        if (keyword != null && !keyword.isEmpty()) {
            productPage = productService.searchApprovedPaged(keyword, pageable);
        } else if (category != null && !category.isEmpty()) {
            productPage = productService.getApprovedByCategoryPaged(category, pageable);
        } else {
            productPage = productService.getAllApproved(pageable);
        }
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("recentPosts", blogService.getPublished().stream().limit(3).toList());
        return "index";
    }
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model, Principal principal) {
        Product product = productService.findById(id).orElse(null);
        if (product == null) return "redirect:/?error=not_found";
        List<Review> reviews = reviewService.getByProduct(product);
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", reviewService.getAverageRating(reviews));
        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("canReview", reviewService.canReview(user, product));
                model.addAttribute("alreadyReviewed", reviewService.hasAlreadyReviewed(user, product));
                model.addAttribute("hasPurchased", reviewService.hasPurchased(user, product));
                model.addAttribute("currentUser", user);
            });
        }
        return "product-detail";
    }
    @PostMapping("/product/{id}/review")
    public String addReview(@PathVariable Long id,
                            @RequestParam int rating,
                            @RequestParam String comment,
                            Principal principal) {
        if (principal == null) return "redirect:/login";
        if (rating < 1 || rating > 5) return "redirect:/product/" + id + "?error=invalid_rating";
        if (comment == null || comment.trim().isEmpty()) return "redirect:/product/" + id + "?error=empty_comment";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        Product product = productService.findById(id).orElse(null);
        if (product == null) return "redirect:/?error=not_found";
        if (!reviewService.canReview(currentUser, product)) return "redirect:/product/" + id + "?error=not_purchased";
        if (reviewService.hasAlreadyReviewed(currentUser, product)) return "redirect:/product/" + id + "?error=already_reviewed";
        reviewService.addReview(product, currentUser, rating, comment);
        return "redirect:/product/" + id + "?review_success";
    }
    @PostMapping("/buyer/order/{id}/received")
    public String markReceived(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        OrderService.DeliverResult result = orderService.markDelivered(id, currentUser.getId());
        return switch (result) {
            case DELIVERED -> "redirect:/buyer/order/" + id + "?received";
            case NOT_FOUND -> "redirect:/buyer/dashboard?error=not_found";
            case UNAUTHORIZED -> "redirect:/buyer/dashboard?error=unauthorized";
            case INVALID_STATUS -> "redirect:/buyer/order/" + id + "?error=invalid_status";
        };
    }
    @PostMapping("/buyer/order/{id}/return")
    public String requestReturn(@PathVariable Long id,
                                @RequestParam(required = false) String returnReason,
                                Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        OrderService.ReturnResult result = orderService.requestReturn(id, currentUser.getId(), returnReason);
        return switch (result) {
            case REQUESTED -> "redirect:/buyer/order/" + id + "?return_requested";
            case NOT_FOUND -> "redirect:/buyer/dashboard?error=not_found";
            case UNAUTHORIZED -> "redirect:/buyer/dashboard?error=unauthorized";
            case INVALID_STATUS -> "redirect:/buyer/order/" + id + "?error=invalid_status";
        };
    }
    @GetMapping("/buyer/dashboard")
    public String buyerDashboard(Model model, Principal principal,
                                 @org.springframework.web.bind.annotation.RequestParam(required = false) String paid) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            if (paid != null) {
                orderService.getUserOrders(user).stream()
                        .filter(o -> "AWAITING_PAYMENT".equals(o.getStatus())
                                && o.getModempayPaymentId() != null)
                        .map(Order::getModempayPaymentId)
                        .distinct()
                        .forEach(modemPayService::verifyAndSync);
            }
            List<Order> orders = orderService.getUserOrders(user);
            if (orders != null) orders.removeIf(Objects::isNull);
            model.addAttribute("orders", orders);
            model.addAttribute("alreadyApplied", vendorApplicationService.hasAlreadyApplied(user));
            List<Notification> notes = userService.getAndMarkNotificationsRead(user);
            model.addAttribute("notifications", notes);
        } else {
            model.addAttribute("alreadyApplied", false);
        }
        return "buyer/dashboard";
    }
    @GetMapping("/buyer/order/{id}/pay")
    public String retryPayment(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        Order order = orderService.findById(id).orElse(null);
        if (order == null || user == null || !order.getUser().getId().equals(user.getId()))
            return "redirect:/buyer/dashboard?error=unauthorized";
        if (!"AWAITING_PAYMENT".equals(order.getStatus()))
            return "redirect:/buyer/dashboard";
        if (!modemPayService.isConfigured())
            return "redirect:/buyer/dashboard?error=payment_unavailable";
        try {
            String paymentUrl = modemPayService.createPaymentIntent(java.util.List.of(order));
            return "redirect:" + paymentUrl;
        } catch (IllegalArgumentException e) {
            return "redirect:/buyer/dashboard?payment_error=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "redirect:/buyer/dashboard?payment_error=" + java.net.URLEncoder.encode("Payment initiation failed. Please try again.", java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    @GetMapping("/buyer/order/{id}")
    public String orderDetails(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        Order order = orderService.findByIdWithItems(id).orElse(null);
        if (order == null || currentUser == null || !order.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/buyer/dashboard?error=unauthorized";
        }
        model.addAttribute("order", order);
        return "buyer/order-details";
    }
    @GetMapping("/settings")
    public String settings(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "settings";
    }
    @PostMapping("/settings/delete-account")
    public String deleteAccount(Principal principal, HttpServletRequest request) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        userService.delete(user.getId());
        return "redirect:/?account_deleted";
    }
    @PostMapping("/buyer/order/delete/{id}")
    public String cancelOrder(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User currentUser = userService.findByEmail(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        OrderService.CancelResult result = orderService.cancelOrder(id, currentUser);
        return switch (result) {
            case CANCELLED -> "redirect:/buyer/dashboard?order_cancelled";
            case NOT_FOUND -> "redirect:/buyer/dashboard?error=not_found";
            case UNAUTHORIZED -> "redirect:/buyer/dashboard?error=unauthorized";
            case CANNOT_CANCEL -> "redirect:/buyer/dashboard?error=cannot_cancel";
        };
    }
    @PostMapping("/subscribe")
    @ResponseBody
    public ResponseEntity<Map<String, String>> subscribe(@RequestParam String email) {
        SubscriberService.Result result = subscriberService.subscribe(email);
        return switch (result) {
            case SUBSCRIBED -> ResponseEntity.ok(Map.of("status", "ok", "msg", "Subscribed! Thank you."));
            case ALREADY_SUBSCRIBED -> ResponseEntity.ok(Map.of("status", "exists", "msg", "You're already subscribed."));
            case INVALID -> ResponseEntity.badRequest().body(Map.of("status", "error", "msg", "Invalid email address."));
        };
    }
    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "privacy-policy";
    }
    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "terms";
    }
    @GetMapping("/cookie-policy")
    public String cookiePolicy(Model model) {
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "cookie-policy";
    }
    @GetMapping("/returns")
    public String returns(Model model) {
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "returns";
    }
}
