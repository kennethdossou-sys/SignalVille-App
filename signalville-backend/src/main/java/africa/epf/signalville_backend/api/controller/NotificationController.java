package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.response.NotificationResponse;
import africa.epf.signalville_backend.api.dto.response.PageResponse;
import africa.epf.signalville_backend.application.mapper.NotificationMapper;
import africa.epf.signalville_backend.application.service.NotificationService;
import africa.epf.signalville_backend.domain.model.Notification;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = notificationService.list(principal, unreadOnly, pageable);
        return ResponseEntity.ok(PageResponse.from(result, NotificationMapper::toResponse));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @PathVariable UUID notificationId) {
        notificationService.markAsRead(principal, notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal AppUserPrincipal principal) {
        notificationService.markAllAsRead(principal);
        return ResponseEntity.noContent().build();
    }
}