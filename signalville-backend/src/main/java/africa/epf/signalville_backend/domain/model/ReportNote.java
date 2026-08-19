package africa.epf.signalville_backend.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Note libre attachée à un signalement pendant son traitement.
 * Le champ `type` détermine la visibilité : une note INTERNE ne doit JAMAIS
 * être renvoyée au citoyen par le service (le filtrage se fait côté
 * NoteService/mapper, jamais en confiant cette responsabilité au frontend —
 * cf. règle CLAUDE_ROLE.md : "ne jamais déplacer une règle de sécurité
 * uniquement côté Angular").
 */
@Entity
@Table(name = "report_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportNote {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NoteType type;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}