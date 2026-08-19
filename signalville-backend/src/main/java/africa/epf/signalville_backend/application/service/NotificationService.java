package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.Notification;
import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.persistence.NotificationRepository;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Notifications in-app. Le module envoi email n'existe pas encore dans le
 * projet : notify() se contente d'un INSERT en base pour l'instant. La methode
 * avale volontairement toute exception : la regle de cadrage S3 est explicite
 * ("l'echec d'un email ne doit jamais annuler l'action metier"), donc meme le
 * futur appel SMTP devra passer par ce point unique et ne jamais remonter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void notify(User recipient, String title, String message, String link) {
        try {
            notificationRepository.save(Notification.builder()
                    .recipient(recipient)
                    .title(title)
                    .message(message)
                    .link(link)
                    .build());
        } catch (Exception ex) {
            // Ne jamais laisser une notification ratee faire echouer la transition
            // metier qui l'a declenchee (assign, resolve, close...).
            log.error("Echec de creation de notification pour {} : {}", recipient.getEmail(), ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(AppUserPrincipal principal, boolean unreadOnly, Pageable pageable) {
        return unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(principal.id(), pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(principal.id(), pageable);
    }

    @Transactional
    public void markAsRead(AppUserPrincipal principal, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        if (!notification.getRecipient().getId().equals(principal.id())) {
            throw new AccessDeniedException("Cette notification n'appartient pas a l'utilisateur courant");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(AppUserPrincipal principal) {
        notificationRepository.markAllAsReadForRecipient(principal.id());
    }
}