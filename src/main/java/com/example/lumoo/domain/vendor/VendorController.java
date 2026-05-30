package com.example.lumoo.domain.vendor;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.order.OrderService;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.security.Principal;
import org.springframework.data.domain.Page;
@Controller
@RequestMapping("/vendor")
public class VendorController {
    @Autowired private ProductService productService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;
    private static final int VENDOR_ORDERS_PAGE_SIZE = 20;
    @GetMapping("/dashboard")
    public String vendorDashboard(Model model,
                                  @AuthenticationPrincipal UserDetails currentUser,
                                  @RequestParam(defaultValue = "0") int page) {
        User vendor = userService.findByEmail(currentUser.getUsername()).orElseThrow();
        Long vendorId = vendor.getId();
        double totalRevenue  = orderService.sumVendorRevenue(vendorId);
        double monthlySales  = orderService.sumVendorMonthlySales(vendorId);
        long   orderCount    = orderService.countVendorOrders(vendorId);
        double averageOrder  = orderCount == 0 ? 0.0 : totalRevenue / orderCount;
        Page<Order> ordersPage = orderService.getVendorOrdersPage(vendorId, page, VENDOR_ORDERS_PAGE_SIZE);
        model.addAttribute("products", productService.getByVendor(vendor));
        model.addAttribute("vendorOrders", ordersPage.getContent());
        model.addAttribute("ordersCurrentPage", ordersPage.getNumber());
        model.addAttribute("ordersTotalPages", ordersPage.getTotalPages());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("averageOrder", averageOrder);
        model.addAttribute("monthlySales", monthlySales);
        model.addAttribute("totalSalesCount", orderCount);
        model.addAttribute("vendorName", vendor.getUsername());
        return "vendor/dashboard";
    }
    @GetMapping("/add-product")
    public String addProductPage(Model model) {
        model.addAttribute("product", new Product());
        return "vendor/add-product";
    }
    @PostMapping("/add-product")
    public String saveProduct(@ModelAttribute Product product,
                              @RequestParam(required = false) MultipartFile image,
                              Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User vendor = userService.findByEmail(principal.getName()).orElse(null);
        if (vendor == null) return "redirect:/login";
        try {
            if (image != null && !image.isEmpty()) {
                product.setImageUrl(productService.saveImage(image));
            }
        } catch (IOException e) {
            ra.addFlashAttribute("flashMsg", "Image upload failed. Please try again.");
            ra.addFlashAttribute("flashType", "red");
            return "redirect:/vendor/add-product";
        }
        productService.addProduct(product, vendor);
        ra.addFlashAttribute("flashMsg", "Product listed. Pending admin approval.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/vendor/dashboard";
    }
    @GetMapping("/edit-product/{id}")
    public String editProductPage(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User vendor = userService.findByEmail(principal.getName()).orElse(null);
        Product product = productService.findById(id).orElse(null);
        if (product == null || vendor == null || !product.getVendor().getId().equals(vendor.getId())) {
            return "redirect:/vendor/dashboard?error=unauthorized";
        }
        model.addAttribute("product", product);
        return "vendor/edit-product";
    }
    @PostMapping("/edit-product/{id}")
    public String updateProduct(@PathVariable Long id,
                                @ModelAttribute Product product,
                                @RequestParam(required = false) MultipartFile image,
                                Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User vendor = userService.findByEmail(principal.getName()).orElse(null);
        if (vendor == null) return "redirect:/login";
        try {
            boolean updated = productService.updateProduct(id, product, vendor, image);
            if (updated) {
                ra.addFlashAttribute("flashMsg", "Product updated successfully.");
                ra.addFlashAttribute("flashType", "green");
                return "redirect:/vendor/dashboard";
            } else {
                ra.addFlashAttribute("flashMsg", "Unauthorized to edit this product.");
                ra.addFlashAttribute("flashType", "red");
                return "redirect:/vendor/dashboard";
            }
        } catch (IOException e) {
            ra.addFlashAttribute("flashMsg", "Image upload failed.");
            ra.addFlashAttribute("flashType", "red");
            return "redirect:/vendor/edit-product/" + id;
        }
    }
    @PostMapping("/order/{id}/ship")
    public String shipOrder(@PathVariable Long id,
                            @RequestParam(required = false) String trackingNumber,
                            @AuthenticationPrincipal UserDetails currentUser,
                            RedirectAttributes ra) {
        User vendor = userService.findByEmail(currentUser.getUsername()).orElseThrow();
        OrderService.ShipResult result = orderService.markShipped(id, vendor.getId(), trackingNumber);
        switch (result) {
            case SHIPPED -> { ra.addFlashAttribute("flashMsg", "Order marked as shipped."); ra.addFlashAttribute("flashType", "green"); }
            case NOT_FOUND -> { ra.addFlashAttribute("flashMsg", "Order not found."); ra.addFlashAttribute("flashType", "red"); }
            case UNAUTHORIZED -> { ra.addFlashAttribute("flashMsg", "Unauthorized action."); ra.addFlashAttribute("flashType", "red"); }
            case INVALID_STATUS -> { ra.addFlashAttribute("flashMsg", "Order cannot be shipped at its current status."); ra.addFlashAttribute("flashType", "red"); }
        }
        return "redirect:/vendor/dashboard";
    }
    @GetMapping("/delete-product/{id}")
    public String deleteProduct(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User vendor = userService.findByEmail(principal.getName()).orElse(null);
        if (vendor == null) return "redirect:/login";
        productService.deleteByVendor(id, vendor);
        ra.addFlashAttribute("flashMsg", "Product deleted.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/vendor/dashboard";
    }
}
