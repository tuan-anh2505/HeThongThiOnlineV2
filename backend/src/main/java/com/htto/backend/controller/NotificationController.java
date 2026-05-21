package com.htto.backend.controller;

import com.htto.backend.dto.response.NotificationReadAllResponse;
import com.htto.backend.dto.response.NotificationResponse;
import com.htto.backend.service.NotificationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getMyNotifications(
            @RequestParam(required = false) Boolean isRead,
            Authentication authentication
    ) {
        return notificationService.getMyNotifications(authentication.getName(), isRead);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(
            @PathVariable String id,
            Authentication authentication
    ) {
        return notificationService.markRead(id, authentication.getName());
    }

    @PatchMapping("/read-all")
    public NotificationReadAllResponse markAllRead(Authentication authentication) {
        return notificationService.markAllRead(authentication.getName());
    }
}
