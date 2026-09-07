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

    @Query("""
            SELECT i FROM Intervention i
            WHERE i.report.id = :reportId
            AND i.status IN (africa.epf.signalville_backend.domain.model.InterventionStatus.AFFECTEE,
                              africa.epf.signalville_backend.domain.model.InterventionStatus.EN_COURS)
            """)
    Optional<Intervention> findActiveByReportId(@Param("reportId") UUID reportId);

    List<Intervention> findByReportIdOrderByAssignedAtDesc(UUID reportId);

    @Query("""
            SELECT i FROM Intervention i
            WHERE i.agent.id = :agentId
            AND i.status IN (africa.epf.signalville_backend.domain.model.InterventionStatus.AFFECTEE,
                              africa.epf.signalville_backend.domain.model.InterventionStatus.EN_COURS)
            ORDER BY i.assignedAt DESC
            """)
    List<Intervention> findActiveByAgentId(@Param("agentId") UUID agentId);

    @Query("""
            SELECT COUNT(i) FROM Intervention i
            WHERE i.agent.id = :agentId
            AND i.status IN (africa.epf.signalville_backend.domain.model.InterventionStatus.AFFECTEE,
                              africa.epf.signalville_backend.domain.model.InterventionStatus.EN_COURS)
            """)
    long countActiveByAgentId(@Param("agentId") UUID agentId);

    @Query("""
            select i.agent.id, i.agent.firstName, i.agent.lastName, count(i)
            from Intervention i
            group by i.agent.id, i.agent.firstName, i.agent.lastName
            """)
    List<Object[]> countAssignedGroupedByAgent();

    @Query("""
            select i.agent.id, count(i)
            from Intervention i
            where i.status = africa.epf.signalville_backend.domain.model.InterventionStatus.RESOLUE
            group by i.agent.id
            """)
    List<Object[]> countResolvedGroupedByAgent();

    @Query("""
            select i.agent.id, count(i)
            from Intervention i
            where i.status = africa.epf.signalville_backend.domain.model.InterventionStatus.REAFFECTEE
            group by i.agent.id
            """)
    List<Object[]> countReassignedGroupedByAgent();

    @Query(value = """
            select agent_id, avg(extract(epoch from (resolved_at - started_at)) / 3600.0)
            from interventions
            where status = 'RESOLUE'
              and resolved_at is not null
              and started_at is not null
            group by agent_id
            """, nativeQuery = true)
    List<Object[]> averageProcessingHoursGroupedByAgent();

    // ------------------------------------------------------------------
    // Module 3 — Dashboard agent (GET /dashboard/agent)
    // ------------------------------------------------------------------

    /**
     * Nombre d'interventions d'un agent dans un statut donne, tous statuts
     * confondus (pas seulement actifs). Utilise pour assignedCount (AFFECTEE)
     * et resolvedCount (RESOLUE) — decision Seance 4 : deux compteurs
     * distincts sans chevauchement, contrairement au superviseur.
     */
    long countByAgentIdAndStatus(UUID agentId, InterventionStatus status);

    /**
     * Delai moyen de traitement effectif (resolvedAt - startedAt, en heures)
     * pour un seul agent — meme calcul que averageProcessingHoursGroupedByAgent
     * mais cible sur un agent, pour eviter de charger toute l'agregation
     * globale juste pour en extraire une ligne dans le dashboard agent.
     */
    @Query(value = """
            select avg(extract(epoch from (resolved_at - started_at)) / 3600.0)
            from interventions
            where agent_id = :agentId
              and status = 'RESOLUE'
              and resolved_at is not null
              and started_at is not null
            """, nativeQuery = true)
    Double averageProcessingHoursForAgent(@Param("agentId") UUID agentId);
}