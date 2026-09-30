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
     * Recupere l'intervention active d'un signalement, avec l'agent et les
     * preuves deja charges (JOIN FETCH). InterventionMapper.toResponse()
     * accede aux deux (agent.getFirstName()/getEmail(), proofs.stream()),
     * et ces deux relations sont LAZY sur Intervention. Sans ce fetch
     * explicite, le mapping (fait dans le controleur, hors de la
     * transaction du service) declenche LazyInitializationException.
     *
     * DISTINCT necessaire : LEFT JOIN FETCH sur une collection (proofs)
     * duplique la ligne principale une fois par preuve associee ; sans
     * DISTINCT, une intervention a 2 preuves reviendrait comme 2 lignes,
     * ce qui ferait echouer le retour en Optional (resultat non unique).
     */
    @Query("""
            SELECT DISTINCT i FROM Intervention i
            JOIN FETCH i.agent
            LEFT JOIN FETCH i.proofs
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
            select agent_id, cast(avg(extract(epoch from (resolved_at - started_at)) / 3600.0) as double precision)
            from interventions
            where status = 'RESOLUE'
              and resolved_at is not null
              and started_at is not null
            group by agent_id
            """, nativeQuery = true)
    List<Object[]> averageProcessingHoursGroupedByAgent();
}