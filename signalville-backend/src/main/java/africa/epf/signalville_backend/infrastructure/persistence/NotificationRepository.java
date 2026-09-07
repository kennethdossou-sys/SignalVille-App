package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Notifications d'un utilisateur, paginées, les plus récentes en premier. */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /** Uniquement les non-lues (paramètre unreadOnly=true du contrat OpenAPI). */
    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /**
     * Marque toutes les notifications d'un utilisateur comme lues en une seule
     * requête UPDATE, plutôt que de charger chaque entité en mémoire pour la
     * modifier une par une (PATCH /notifications/read-all).
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    void markAllAsReadForRecipient(@Param("recipientId") UUID recipientId);

    /**
     * Nombre de notifications non lues d'un utilisateur. Utilise pour
     * unreadNotifications sur les dashboards (citoyen, agent) sans charger
     * les entites en memoire — juste un COMPTAGE.
     */
    long countByRecipientIdAndReadFalse(UUID recipientId);
}