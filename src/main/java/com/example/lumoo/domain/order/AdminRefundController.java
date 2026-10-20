package com.example.lumoo.domain.order;
import com.example.lumoo.domain.pdpp.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.Map;
/** Admin refunds: approve a (partial) refund for an order, then record the transfer once it is paid. */
@Controller
@RequestMapping("/admin/refunds")
public class AdminRefundController {
    @Autowired private RefundService refundService;
    @Autowired private OrderService orderService;
    @Autowired private AuditService auditService;
    @GetMapping
    public String list(Model model) {
        model.addAttribute("pending", refundService.getPending());
        model.addAttribute("completed", refundService.getCompleted());
        return "admin/refunds";
    }
    /** New-refund form. Submitting it with GET (the Preview button) shows the server-computed amounts. */
    @GetMapping("/new")
    public String form(@RequestParam Long orderId, @RequestParam Map<String, String> params,
                       Model model, RedirectAttributes ra) {
        Order order = orderService.findByIdWithItems(orderId).orElse(null);
        if (order == null) return flash(ra, "Order not found.", "red", "redirect:/admin/refunds");
        model.addAttribute("order", order);
        model.addAttribute("refundable", refundService.isRefundable(order));
        model.addAttribute("history", refundService.getForOrder(orderId));
        model.addAttribute("reason", params.get("reason"));
        boolean shipping = params.containsKey("shipping");
        model.addAttribute("shipping", shipping);
        Map<Long, Integer> qty;
        try {
            qty = RefundService.parseQuantities(params);
        } catch (NumberFormatException e) {
            qty = Map.of();
        }
        model.addAttribute("qty", qty);
        if (params.containsKey("preview")) {
            try {
                model.addAttribute("quote", refundService.quote(orderId, qty, shipping));
            } catch (RefundService.RefundException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "admin/refund-form";
    }
    @PostMapping
    public String create(@RequestParam Long orderId, @RequestParam(required = false) String reason,
                         @RequestParam Map<String, String> params,
                         Principal principal, HttpServletRequest request, RedirectAttributes ra) {
        String back = "redirect:/admin/refunds/new?orderId=" + orderId;
        try {
            Refund r = refundService.create(orderId, RefundService.parseQuantities(params),
                    params.containsKey("shipping"), reason, principal != null ? principal.getName() : null);
            auditService.log("REFUND_APPROVED", "Refund", String.valueOf(r.getId()), null,
                    Map.of("orderId", orderId, "amount", r.getAmount(), "vendorDebt", r.getVendorDebt()), request);
            return flash(ra, String.format("Refund of GMD %.2f approved. Pay it by %s, then record the reference.",
                    r.getAmount(), r.getDueBy()), "green", "redirect:/admin/refunds");
        } catch (NumberFormatException e) {
            return flash(ra, "Quantities must be whole numbers.", "red", back);
        } catch (RefundService.RefundException e) {
            return flash(ra, e.getMessage(), "red", back);
        }
    }
    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, @RequestParam(required = false) String method,
                           @RequestParam(required = false) String reference,
                           Principal principal, HttpServletRequest request, RedirectAttributes ra) {
        RefundService.CompleteResult r = refundService.complete(id, method, reference, principal != null ? principal.getName() : null);
        if (r == RefundService.CompleteResult.COMPLETED)
            auditService.log("REFUND_COMPLETED", "Refund", String.valueOf(id), null,
                    Map.of("method", method != null ? method : "", "reference", reference), request);
        return flash(ra, switch (r) {
            case COMPLETED -> "Refund marked as sent. The buyer has been emailed.";
            case REFERENCE_REQUIRED -> "Enter the transfer reference so the payment can be traced.";
            case ALREADY_COMPLETED -> "This refund was already completed.";
            case NOT_FOUND -> "Refund not found.";
        }, r == RefundService.CompleteResult.COMPLETED ? "green" : "red", "redirect:/admin/refunds");
    }
    private static String flash(RedirectAttributes ra, String msg, String type, String target) {
        ra.addFlashAttribute("flashMsg", msg);
        ra.addFlashAttribute("flashType", type);
        return target;
    }
}
