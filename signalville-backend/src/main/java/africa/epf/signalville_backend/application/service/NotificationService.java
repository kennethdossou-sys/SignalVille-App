package africa.epf.signalville_backend.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import africa.epf.signalville_backend.domain.model.Notification;
import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;

/**
 * Contrat des notifications in-app. L'implementation vit dans
 * application.service.impl.NotificationServiceImpl.
 *
 * notify() ne doit jamais faire echouer la transition metier qui l'a
 * declenchee (assign, resolve, close...) — voir l'implementation pour le
 * detail de cette garantie.
 */
public interface NotificationService {

    void notify(User recipient, String title, String message, String link);

    Page<Notification> list(AppUserPrincipal principal, boolean unreadOnly, Pageable pageable);

    void markAsRead(AppUserPrincipal principal, UUID notificationId);

    void markAllAsRead(AppUserPrincipal principal);
}