package com.example.lumoo.domain.user;

import com.example.lumoo.shared.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    @Autowired private UserService userService;
    @Autowired private NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<?> list(Principal principal,
                                  @RequestParam(name = "continue", required = false) Long beforeId) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        List<Notification> page = beforeId != null
                ? notificationRepository.findByUserAndIdLessThanOrderByCreatedAtDesc(
                        user, beforeId, PageRequest.of(0, PAGE_SIZE + 1))
                : notificationRepository.findByUserOrderByCreatedAtDesc(
                        user, PageRequest.of(0, PAGE_SIZE + 1));

        boolean hasMore = page.size() > PAGE_SIZE;
        List<Notification> batch = hasMore ? page.subList(0, PAGE_SIZE) : page;

        List<Map<String, Object>> items = batch.stream()
                .map(n -> Map.<String, Object>of(
                        "id",        n.getId(),
                        "message",   n.getMessage(),
                        "read",      n.isRead(),
                        "createdAt", n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : ""
                ))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("unread", userService.countUnreadNotifications(user));
        result.put("items", items);
        if (hasMore) result.put("continue", batch.get(batch.size() - 1).getId());

        return ResponseEntity.ok(result);
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
