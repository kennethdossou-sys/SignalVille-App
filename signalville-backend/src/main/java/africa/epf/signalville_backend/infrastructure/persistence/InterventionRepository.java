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

    // ------------------------------------------------------------------
    // Module 3 — Statistiques par agent (GET /statistics/agents)
    // ------------------------------------------------------------------

    /**
     * Liste des agents ayant au moins une intervention, avec leur nombre
     * total d'interventions affectées. Sert de base pour construire
     * AgentStatisticsResponse : les agents sans aucune intervention sont
     * naturellement absents (decision Seance 4 : leur absence communique
     * deja l'information, pas besoin d'une ligne a zero).
     * Chaque ligne du resultat : [UUID agentId, String firstName, String lastName, Long assignedCount].
     */
    @Query("""
            select i.agent.id, i.agent.firstName, i.agent.lastName, count(i)
            from Intervention i
            group by i.agent.id, i.agent.firstName, i.agent.lastName
            """)
    List<Object[]> countAssignedGroupedByAgent();

    /**
     * Nombre d'interventions RESOLUE par agent (i.e. menees a bien par cet
     * agent, independamment de si le signalement a ensuite ete cloture ou
     * rouvert). Chaque ligne du resultat : [UUID agentId, Long resolvedCount].
     */
    @Query("""
            select i.agent.id, count(i)
            from Intervention i
            where i.status = africa.epf.signalville_backend.domain.model.InterventionStatus.RESOLUE
            group by i.agent.id
            """)
    List<Object[]> countResolvedGroupedByAgent();

    /**
     * Nombre d'interventions REAFFECTEE par agent : dossiers qui ont ete
     * retires a cet agent pour etre confies a quelqu'un d'autre (decision
     * Seance 4, option (a)). L'intervention REAFFECTEE conserve son agent
     * d'origine (celui qui a ete dessaisi), voir InterventionService.reassign().
     * Chaque ligne du resultat : [UUID agentId, Long reassignedCount].
     */
    @Query("""
            select i.agent.id, count(i)
            from Intervention i
            where i.status = africa.epf.signalville_backend.domain.model.InterventionStatus.REAFFECTEE
            group by i.agent.id
            """)
    List<Object[]> countReassignedGroupedByAgent();

    /**
     * Delai moyen de traitement effectif par agent, en heures : resolvedAt
     * moins startedAt, calcule uniquement sur les interventions RESOLUE
     * (les deux dates sont garanties non nulles a ce statut). Decision
     * Seance 4 : le temps de travail reel de l'agent, pas le delai total
     * incluant l'attente avant demarrage (voir assignedAt vs startedAt).
     *
     * EXTRACT(EPOCH FROM ...) est une fonction PostgreSQL, pas JPQL standard :
     * requete native necessaire ici. Parametres lies (aucune concatenation
     * de texte), meme niveau de securite qu'une requete JPQL.
     */
    @Query(value = """
            select agent_id, avg(extract(epoch from (resolved_at - started_at)) / 3600.0)
            from interventions
            where status = 'RESOLUE'
              and resolved_at is not null
              and started_at is not null
            group by agent_id
            """, nativeQuery = true)
    List<Object[]> averageProcessingHoursGroupedByAgent();
}