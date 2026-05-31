package com.example.lumoo.domain.user;

import com.example.lumoo.shared.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired private UserService userService;
    @Autowired private NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<?> list(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        long unread = userService.countUnreadNotifications(user);
        List<Map<String, Object>> items = userService.getRecentNotifications(user).stream()
                .map(n -> Map.<String, Object>of(
                        "id",        n.getId(),
                        "message",   n.getMessage(),
                        "read",      n.isRead(),
                        "createdAt", n.getCreatedAt() != null
                                ? n.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
                                : ""
                ))
                .toList();

        return ResponseEntity.ok(Map.of("unread", unread, "items", items));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        List<Notification> unread = notificationRepository.findByUserAndIsReadFalse(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);

        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read.", null));
    }
}
