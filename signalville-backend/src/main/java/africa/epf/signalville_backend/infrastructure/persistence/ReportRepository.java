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

    @Query("select r.status, count(r) from Report r group by r.status")
    List<Object[]> countGroupedByStatus();

    @Query("select r.category.name, count(r) from Report r group by r.category.name")
    List<Object[]> countGroupedByCategory();

    @Query("""
            select r.district, count(r) from Report r
            where r.district is not null
            group by r.district
            """)
    List<Object[]> countGroupedByDistrict();

    @Query("select count(r) from Report r where r.createdAt >= :since")
    long countCreatedSince(@Param("since") LocalDateTime since);

    long countByStatus(ReportStatus status);

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

    List<Report> findByStatusOrderByUpdatedAtAsc(ReportStatus status);

    // ------------------------------------------------------------------
    // Module 3 — Timeline (GET /statistics/timeline)
    // ------------------------------------------------------------------

    /**
     * Nombre de signalements crees, groupes par periode (jour/semaine/mois
     * selon granularity). date_trunc est une fonction PostgreSQL native,
     * pas JPQL standard : requete native necessaire. granularity et les
     * bornes de date restent des parametres lies (aucune concatenation de
     * texte), meme niveau de securite qu'une requete JPQL.
     * Chaque ligne du resultat : [Timestamp period, Long count].
     */
    @Query(value = """
            select date_trunc(:granularity, created_at) as period, count(*) as cnt
            from reports
            where created_at >= :dateFrom and created_at < :dateToExclusive
            group by period
            order by period
            """, nativeQuery = true)
    List<Object[]> countCreatedGroupedByPeriod(@Param("granularity") String granularity,
                                                @Param("dateFrom") LocalDateTime dateFrom,
                                                @Param("dateToExclusive") LocalDateTime dateToExclusive);
}