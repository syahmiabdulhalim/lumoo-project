package com.example.lumoo.domain.pdpp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.time.LocalDateTime;
/** Admin breach register (PDPP). Regulator notification is an explicit, separate admin action. */
@Controller
@RequestMapping("/admin/breaches")
public class AdminBreachController {
    @Autowired private DataBreachService dataBreachService;
    @GetMapping
    public String list(@RequestParam(required = false) Long view, Model model) {
        model.addAttribute("incidents", dataBreachService.getAll());
        model.addAttribute("statuses", BreachIncident.Status.values());
        model.addAttribute("regulatorEnabled", dataBreachService.isRegulatorSendingEnabled());
        model.addAttribute("regulatorEmail", dataBreachService.getRegulatorEmail());
        if (view != null) dataBreachService.findById(view).ifPresent(i -> {
            model.addAttribute("selected", i);
            model.addAttribute("regulatorPreview", dataBreachService.buildRegulatorEmail(i));
        });
        return "admin/breaches";
    }
    @PostMapping
    public String report(@RequestParam String description,
                         @RequestParam(required = false) String affectedData,
                         @RequestParam(defaultValue = "0") int affectedUsers,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime discoveredAt,
                         @RequestParam(required = false) String containmentActions,
                         Principal principal, HttpServletRequest request, RedirectAttributes ra) {
        if (description == null || description.isBlank()) {
            ra.addFlashAttribute("flashMsg", "Description is required.");
            ra.addFlashAttribute("flashType", "red");
            return "redirect:/admin/breaches";
        }
        BreachIncident i = dataBreachService.reportBreach(description.trim(), affectedData, Math.max(0, affectedUsers),
                discoveredAt != null ? discoveredAt : LocalDateTime.now(), containmentActions,
                principal != null ? principal.getName() : null, request);
        ra.addFlashAttribute("flashMsg", "Breach #" + i.getId() + " recorded. Notify the regulator before the 72-hour deadline.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/admin/breaches?view=" + i.getId();
    }
    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam BreachIncident.Status status, RedirectAttributes ra) {
        dataBreachService.updateStatus(id, status);
        ra.addFlashAttribute("flashMsg", "Status updated.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/breaches?view=" + id;
    }
    @PostMapping("/{id}/notify-regulator")
    public String notifyRegulator(@PathVariable Long id, Principal principal, HttpServletRequest request, RedirectAttributes ra) {
        DataBreachService.NotifyResult r = dataBreachService.notifyRegulator(id, principal != null ? principal.getName() : null, request);
        ra.addFlashAttribute("flashMsg", switch (r) {
            case SENT -> "Notification sent to the Information Commission.";
            case ALREADY_SENT -> "The regulator was already notified for this incident.";
            case DISABLED -> "Sending is disabled until the regulator's address is confirmed — send the preview manually.";
            case SEND_FAILED -> "Email could not be sent. Send the preview manually and keep proof.";
            case NOT_FOUND -> "Incident not found.";
        });
        ra.addFlashAttribute("flashType", r == DataBreachService.NotifyResult.SENT ? "green" : "red");
        return "redirect:/admin/breaches?view=" + id;
    }
}
