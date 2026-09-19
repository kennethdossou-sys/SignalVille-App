package africa.epf.signalville_backend.application.service.impl;

import africa.epf.signalville_backend.application.service.NotificationService;
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
 *
 * notify() n'est volontairement pas @Transactional : si elle l'etait et
 * echouait, elle pourrait faire echouer par rollback la transaction
 * englobante (ex: InterventionService.assign()). Le try/catch interne est la
 * vraie protection ; l'absence de @Transactional en est une seconde, par
 * prudence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void notify(User recipient, String title, String message, String link) {
        try {
            notificationRepository.save(Notification.builder()
                    .recipient(recipient)
                    .title(title)
                    .message(message)
                    .link(link)
                    .build());
        } catch (Exception ex) {
            log.error("Echec de creation de notification pour {} : {}", recipient.getEmail(), ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> list(AppUserPrincipal principal, boolean unreadOnly, Pageable pageable) {
        return unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(principal.id(), pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(principal.id(), pageable);
    }

    @Override
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

    @Override
    @Transactional
    public void markAllAsRead(AppUserPrincipal principal) {
        notificationRepository.markAllAsReadForRecipient(principal.id());
    }
}