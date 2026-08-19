package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.Intervention;
import africa.epf.signalville_backend.domain.model.InterventionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterventionRepository extends JpaRepository<Intervention, UUID> {

    /**
     * Récupère l'intervention actuellement "active" (AFFECTEE ou EN_COURS) d'un
     * signalement, s'il y en a une. Reflète exactement la contrainte posée en
     * base par l'index unique partiel ux_interventions_one_active_per_report :
     * il ne peut y en avoir qu'une seule, donc Optional est le bon type de retour.
     */
    @Query("""
            SELECT i FROM Intervention i
            WHERE i.report.id = :reportId
            AND i.status IN (africa.epf.signalville_backend.domain.model.InterventionStatus.AFFECTEE,
                              africa.epf.signalville_backend.domain.model.InterventionStatus.EN_COURS)
            """)
    Optional<Intervention> findActiveByReportId(@Param("reportId") UUID reportId);

    /**
     * Historique complet des interventions d'un signalement (utile pour audit /
     * détail enrichi), triées de la plus récente à la plus ancienne.
     */
    List<Intervention> findByReportIdOrderByAssignedAtDesc(UUID reportId);

    /**
     * Interventions actuellement affectées à un agent donné, dans un statut actif.
     * Utilisé pour "mes signalements à traiter" côté agent.
     */
    @Query("""
            SELECT i FROM Intervention i
            WHERE i.agent.id = :agentId
            AND i.status IN (africa.epf.signalville_backend.domain.model.InterventionStatus.AFFECTEE,
                              africa.epf.signalville_backend.domain.model.InterventionStatus.EN_COURS)
            ORDER BY i.assignedAt DESC
            """)
    List<Intervention> findActiveByAgentId(@Param("agentId") UUID agentId);

    /**
     * Nombre d'interventions actives d'un agent — utilisé par le superviseur pour
     * connaître "sa charge courante" avant de choisir à qui affecter (exigence
     * explicite du cadrage S3, section Affectation).
     */
    @Query("""
            SELECT COUNT(i) FROM Intervention i
            WHERE i.agent.id = :agentId
            AND i.status IN (africa.epf.signalville_backend.domain.model.InterventionStatus.AFFECTEE,
                              africa.epf.signalville_backend.domain.model.InterventionStatus.EN_COURS)
            """)
    long countActiveByAgentId(@Param("agentId") UUID agentId);
}