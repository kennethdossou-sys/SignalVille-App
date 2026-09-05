package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.Priority;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    @Query("select r from Report r join fetch r.category left join fetch r.photos where r.id = :id")
    Optional<Report> findDetailById(@Param("id") UUID id);

    /**
     * Recherche paginee restreinte a un citoyen. Les filtres optionnels sont
     * neutralises lorsqu'ils valent null, ce qui evite une Specification pour un besoin simple.
     */
    @Query("""
            select r from Report r
            where r.citizen.id = :citizenId
              and (:status     is null or r.status   = :status)
              and (:categoryId is null or r.category.id = :categoryId)
              and (:priority   is null or r.priority = :priority)
              and (cast(:search as string) is null
                   or lower(r.title)     like lower(concat('%', cast(:search as string), '%'))
                   or lower(r.reference) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Report> searchForCitizen(@Param("citizenId") UUID citizenId,
                                  @Param("status") ReportStatus status,
                                  @Param("categoryId") UUID categoryId,
                                  @Param("priority") Priority priority,
                                  @Param("search") String search,
                                  Pageable pageable);

    /** Vue elargie (agent, superviseur, administrateur) : pas de restriction de propriete. */
    @Query("""
            select r from Report r
            where (:status     is null or r.status   = :status)
              and (:categoryId is null or r.category.id = :categoryId)
              and (:priority   is null or r.priority = :priority)
              and (cast(:search as string) is null
                   or lower(r.title)     like lower(concat('%', cast(:search as string), '%'))
                   or lower(r.reference) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Report> searchAll(@Param("status") ReportStatus status,
                           @Param("categoryId") UUID categoryId,
                           @Param("priority") Priority priority,
                           @Param("search") String search,
                           Pageable pageable);

    // ------------------------------------------------------------------
    // Module 3 — Statistiques et tableaux de bord
    //
    // NOTE : le calcul de averageProcessingHours / targetComplianceRate
    // n'est volontairement pas ici. Il depend de StatusHistory (source de
    // verite retenue pour la date de cloture), voir StatusHistoryRepository.
    // ------------------------------------------------------------------

    /**
     * Repartition du nombre de signalements par statut, tous roles confondus.
     * Alimente StatisticsResponse.byStatus.
     * Chaque ligne du resultat : [ReportStatus status, Long count].
     */
    @Query("select r.status, count(r) from Report r group by r.status")
    List<Object[]> countGroupedByStatus();

    /**
     * Repartition du nombre de signalements par categorie.
     * Alimente StatisticsResponse.byCategory.
     * Chaque ligne du resultat : [String categoryName, Long count].
     */
    @Query("select r.category.name, count(r) from Report r group by r.category.name")
    List<Object[]> countGroupedByCategory();

    /**
     * Repartition du nombre de signalements par quartier. Les signalements
     * sans quartier renseigne (champ optionnel) sont exclus plutot que
     * comptes sous une cle null, pour ne pas polluer l'agregat cote front.
     * Alimente StatisticsResponse.byDistrict et SupervisorDashboardResponse.byDistrict.
     * Chaque ligne du resultat : [String district, Long count].
     */
    @Query("""
            select r.district, count(r) from Report r
            where r.district is not null
            group by r.district
            """)
    List<Object[]> countGroupedByDistrict();

    /**
     * Nombre de signalements crees dans les dernieres 24h, tous statuts confondus.
     * Indicateur de flux entrant, distinct du stock a affecter (voir countByStatus
     * pour NOUVEAU/REOUVERT). Nomme volontairement last24HoursCount et non
     * "newCount", pour ne pas entrer en collision de sens avec ReportStatus.NOUVEAU.
     */
    @Query("select count(r) from Report r where r.createdAt >= :since")
    long countCreatedSince(@Param("since") LocalDateTime since);

    /**
     * Nombre de signalements dans un statut donne. Utilise pour unassignedCount
     * (NOUVEAU + REOUVERT), reopenedCount (REOUVERT), inProgressCount
     * (AFFECTE + EN_COURS), closedCount (CLOTURE) — sans dupliquer une requete
     * de comptage groupe deja disponible via countGroupedByStatus, quand un
     * seul statut est necessaire.
     */
    long countByStatus(ReportStatus status);

    /**
     * Signalements actuellement critiques : priorite effective de l'instance
     * (Report.priority) egale a CRITIQUE, source de verite retenue plutot que
     * Category.defaultPriority (decision actee en Seance 4). Exclut les etats
     * terminaux : un signalement CLOTURE, REJETE ou ANNULE n'est plus une
     * urgence a traiter, quelle que soit sa priorite.
     * Alimente SupervisorDashboardResponse.criticalCount / criticalReports.
     */
    @Query("""
            select r from Report r
            where r.priority = africa.epf.signalville_backend.domain.model.Priority.CRITIQUE
              and r.status not in (
                    africa.epf.signalville_backend.domain.model.ReportStatus.CLOTURE,
                    africa.epf.signalville_backend.domain.model.ReportStatus.REJETE,
                    africa.epf.signalville_backend.domain.model.ReportStatus.ANNULE)
            order by r.createdAt desc
            """)
    List<Report> findCriticalActive();

    /**
     * Signalements dans un statut donne, du plus ancien au plus recent selon
     * leur derniere modification. Utilise pour reportsToVerify (RESOLU) et
     * reopenedReports (REOUVERT) : dans les deux cas, les dossiers en attente
     * depuis le plus longtemps remontent en premier.
     */
    List<Report> findByStatusOrderByUpdatedAtAsc(ReportStatus status);
}