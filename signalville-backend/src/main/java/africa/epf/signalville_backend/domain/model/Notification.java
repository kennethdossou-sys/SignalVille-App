package africa.epf.signalville_backend.domain.model;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification in-app destinée à un utilisateur (citoyen, agent ou superviseur
 * selon l'événement). Volontairement découplée de l'envoi d'email : la création
 * de cette ligne ne doit jamais dépendre de la réussite d'un envoi SMTP, et
 * inversement, un échec d'email ne doit jamais empêcher la transition métier
 * qui l'a déclenché (règle explicite du cadrage S3).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Le destinataire de la notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** Lien relatif optionnel vers la ressource concernée (ex: /reports/{id}). */
    @Column(name = "link", length = 300)
    private String link;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}