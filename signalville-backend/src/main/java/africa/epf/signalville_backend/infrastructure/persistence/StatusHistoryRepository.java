package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

    List<StatusHistory> findByReportIdOrderByChangedAtAsc(UUID reportId);

    @Query("""
            select sh.report.id, max(sh.changedAt)
            from StatusHistory sh
            where sh.newStatus = africa.epf.signalville_backend.domain.model.ReportStatus.CLOTURE
            group by sh.report.id
            """)
    List<Object[]> findLastClosedAtByReportId();

    // ------------------------------------------------------------------
    // Module 3 — Timeline (GET /statistics/timeline)
    // ------------------------------------------------------------------

    /**
     * Nombre de transitions vers RESOLU, groupees par periode. resolvedCount
     * de la timeline compte les passages a RESOLU (l'agent a resolu), pas
     * les CLOTURE (verification superviseur) — decision Seance 4.
     * Chaque ligne du resultat : [Timestamp period, Long count].
     */
    @Query(value = """
            select date_trunc(:granularity, changed_at) as period, count(*) as cnt
            from report_status_history
            where new_status = 'RESOLU'
              and changed_at >= :dateFrom and changed_at < :dateToExclusive
            group by period
            order by period
            """, nativeQuery = true)
    List<Object[]> countResolvedGroupedByPeriod(@Param("granularity") String granularity,
                                                 @Param("dateFrom") LocalDateTime dateFrom,
                                                 @Param("dateToExclusive") LocalDateTime dateToExclusive);
}