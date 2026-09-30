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

    // ------------------------------------------------------------------
    // Module 3 — Dashboard citoyen (GET /dashboard/citizen)
    // ------------------------------------------------------------------

    long countByCitizenIdAndStatusIn(UUID citizenId, List<ReportStatus> statuses);

    @Query("select r.status, count(r) from Report r where r.citizen.id = :citizenId group by r.status")
    List<Object[]> countGroupedByStatusForCitizen(@Param("citizenId") UUID citizenId);

    List<Report> findByCitizenIdOrderByCreatedAtDesc(UUID citizenId, Pageable pageable);

    // ------------------------------------------------------------------
    // Module 3 — Filtrage temporel (GET /statistics/general)
    // ------------------------------------------------------------------

    /**
     * Variantes filtrees par plage de creation des methodes d'agregation
     * generales. dateFrom/dateTo nullable et null-safe (meme pattern que
     * searchAll) : absence de bornes = comportement identique aux methodes
     * non filtrees. dateToExclusive est une borne exclusive (jour suivant
     * a minuit), pour inclure toute la journee de dateTo.
     *
     * cast(:param as timestamp) est necessaire (pas juste :param is null) :
     * PostgreSQL refuse d'inferer le type d'un parametre nu compare
     * uniquement via "is null" (SQLState 42P18, "could not determine data
     * type of parameter"). Le cast force Hibernate a typer explicitement
     * le parametre dans le SQL genere. Meme necessite que cast(:search as
     * string) plus haut, deja en place pour les memes raisons.
     */
    @Query("""
            select count(r) from Report r
            where (cast(:dateFrom as timestamp) is null or r.createdAt >= :dateFrom)
              and (cast(:dateToExclusive as timestamp) is null or r.createdAt < :dateToExclusive)
            """)
    long countInRange(@Param("dateFrom") LocalDateTime dateFrom,
                       @Param("dateToExclusive") LocalDateTime dateToExclusive);

    @Query("""
            select r.status, count(r) from Report r
            where (cast(:dateFrom as timestamp) is null or r.createdAt >= :dateFrom)
              and (cast(:dateToExclusive as timestamp) is null or r.createdAt < :dateToExclusive)
            group by r.status
            """)
    List<Object[]> countGroupedByStatusInRange(@Param("dateFrom") LocalDateTime dateFrom,
                                                @Param("dateToExclusive") LocalDateTime dateToExclusive);

    @Query("""
            select r.category.name, count(r) from Report r
            where (cast(:dateFrom as timestamp) is null or r.createdAt >= :dateFrom)
              and (cast(:dateToExclusive as timestamp) is null or r.createdAt < :dateToExclusive)
            group by r.category.name
            """)
    List<Object[]> countGroupedByCategoryInRange(@Param("dateFrom") LocalDateTime dateFrom,
                                                  @Param("dateToExclusive") LocalDateTime dateToExclusive);

    @Query("""
            select r.district, count(r) from Report r
            where r.district is not null
              and (cast(:dateFrom as timestamp) is null or r.createdAt >= :dateFrom)
              and (cast(:dateToExclusive as timestamp) is null or r.createdAt < :dateToExclusive)
            group by r.district
            """)
    List<Object[]> countGroupedByDistrictInRange(@Param("dateFrom") LocalDateTime dateFrom,
                                                  @Param("dateToExclusive") LocalDateTime dateToExclusive);
}