package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

    List<StatusHistory> findByReportIdOrderByChangedAtAsc(UUID reportId);

    /**
     * Date de la dernière transition vers CLOTURE, par signalement.
     *
     * MAX(changedAt) et non MIN : un signalement peut être réouvert puis
     * reclôturé. Prendre la première clôture sous-estimerait le délai réel
     * de traitement définitif.
     *
     * Source de vérité retenue pour averageProcessingHours / targetComplianceRate
     * (StatisticsResponse) : changedAt est immuable (@CreationTimestamp,
     * updatable = false), contrairement à Report.updatedAt qui peut dériver
     * après la clôture (ex: ajout tardif d'un commentaire).
     */
    @Query("""
            select sh.report.id, max(sh.changedAt)
            from StatusHistory sh
            where sh.newStatus = africa.epf.signalville_backend.domain.model.ReportStatus.CLOTURE
            group by sh.report.id
            """)
    List<Object[]> findLastClosedAtByReportId();
}