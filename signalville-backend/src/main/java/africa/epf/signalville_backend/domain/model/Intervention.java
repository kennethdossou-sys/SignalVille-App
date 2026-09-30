package africa.epf.signalville_backend.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Représente le cycle de traitement terrain d'un signalement par un agent.
 *
 * Cycle de vie : AFFECTEE -> EN_COURS -> RESOLUE
 *                AFFECTEE|EN_COURS -> REAFFECTEE (si le superviseur réaffecte)
 *                AFFECTEE|EN_COURS -> INTERROMPUE (interruption sans réaffectation)
 *
 * Contrainte d'unicité : un même signalement (report_id) ne peut avoir qu'une seule
 * intervention dans un statut "actif" (AFFECTEE ou EN_COURS) à la fois. Cette règle
 * est garantie au niveau base de données (index unique partiel, migration V6), pas
 * seulement ici en Java : le service ne doit pas être l'unique rempart contre une
 * incohérence de concurrence (double affectation simultanée par exemple).
 */
@Entity
@Table(name = "interventions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intervention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Le signalement concerné. Chargement LAZY : on ne veut pas charger tout le
     * Report à chaque fois qu'on manipule une Intervention (ex: liste des
     * interventions d'un agent) — seulement quand le code y accède explicitement.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    /** L'agent de terrain désigné pour traiter cette intervention. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InterventionStatus status = InterventionStatus.AFFECTEE;

    /** Instruction facultative donnée par le superviseur au moment de l'affectation. */
    @Column(name = "instruction", length = 500)
    private String instruction;

    /** Commentaire obligatoire fourni par l'agent au moment de la résolution. */
    @Column(name = "resolution_comment", length = 500)
    private String resolutionComment;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    /** Renseigné uniquement au passage EN_COURS. */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** Renseigné uniquement au passage RESOLUE. */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Preuves photo de résolution (1 à 3, contrainte vérifiée en service avant
     * persistance, pas seulement côté DTO). Cascade ALL + orphanRemoval : les preuves
     * n'ont aucun sens en dehors de leur intervention, elles suivent son cycle de vie.
     */
    @OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InterventionProof> proofs = new ArrayList<>();
}