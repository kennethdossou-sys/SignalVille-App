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
 * Photo de preuve accompagnant la résolution d'une intervention.
 * Structure calquée sur ReportPhoto (même stratégie de stockage disque,
 * seul le chemin est conservé en base) — volontairement pas de fusion avec
 * ReportPhoto : ce sont deux moments métier distincts (preuve de dépôt du
 * signalement vs preuve de résolution), avec des cycles de vie indépendants.
 */
@Entity
@Table(name = "intervention_proofs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterventionProof {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intervention_id", nullable = false)
    private Intervention intervention;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "description")
    private String description;

    /** Position d'affichage, entre 1 et 3 (contrainte CHECK en base). */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}