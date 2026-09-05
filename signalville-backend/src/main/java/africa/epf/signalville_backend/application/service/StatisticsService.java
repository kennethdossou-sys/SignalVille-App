package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.StatisticsResponse;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.infrastructure.persistence.ReportRepository;
import africa.epf.signalville_backend.infrastructure.persistence.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calcule les statistiques générales de la plateforme (GET /statistics/general).
 *
 * Le délai de traitement d'un signalement clôturé se base sur la date réelle
 * de la dernière transition vers CLOTURE (StatusHistory), jamais sur
 * Report.updatedAt — donnée technique modifiable après la clôture (ex: ajout
 * tardif d'un commentaire public) qui fausserait silencieusement les
 * statistiques historiques. Décision actée en Séance 4 (Option B).
 *
 * En cas de réouverture puis re-clôture, c'est la clôture la plus récente qui
 * est retenue (voir StatusHistoryRepository.findLastClosedAtByReportId).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final ReportRepository reportRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public StatisticsResponse getGeneralStatistics() {
        long totalReports = reportRepository.count();

        Map<String, Long> byStatus = toStringKeyedMap(reportRepository.countGroupedByStatus());
        Map<String, Long> byCategory = toStringKeyedMap(reportRepository.countGroupedByCategory());
        Map<String, Long> byDistrict = toStringKeyedMap(reportRepository.countGroupedByDistrict());

        ProcessingMetrics metrics = computeProcessingMetrics();

        return new StatisticsResponse(
                totalReports,
                byStatus,
                byCategory,
                byDistrict,
                metrics.averageProcessingHours(),
                metrics.targetComplianceRate());
    }

    /**
     * Croise, pour chaque signalement CLOTURE, sa date de création avec la
     * date réelle de sa dernière clôture (StatusHistory), puis compare le
     * délai obtenu au délai cible de sa catégorie (targetDelayHours).
     */
    private ProcessingMetrics computeProcessingMetrics() {
        List<Report> closedReports = reportRepository
                .searchAll(ReportStatus.CLOTURE, null, null, null, Pageable.unpaged())
                .getContent();

        if (closedReports.isEmpty()) {
            return new ProcessingMetrics(0.0, 0.0);
        }

        Map<UUID, LocalDateTime> lastClosedAtByReportId = new HashMap<>();
        for (Object[] row : statusHistoryRepository.findLastClosedAtByReportId()) {
            lastClosedAtByReportId.put((UUID) row[0], (LocalDateTime) row[1]);
        }

        double totalHours = 0.0;
        long compliantCount = 0;
        long measuredCount = 0;

        for (Report report : closedReports) {
            LocalDateTime lastClosedAt = lastClosedAtByReportId.get(report.getId());
            if (lastClosedAt == null) {
                // Incohérence de données (signalement CLOTURE sans trace dans
                // l'historique) : ignoré plutôt que de fausser la moyenne
                // avec une valeur par défaut arbitraire.
                continue;
            }

            long hours = ChronoUnit.HOURS.between(report.getCreatedAt(), lastClosedAt);
            totalHours += hours;
            measuredCount++;

            Integer targetDelayHours = report.getCategory().getTargetDelayHours();
            if (targetDelayHours != null && hours <= targetDelayHours) {
                compliantCount++;
            }
        }

        if (measuredCount == 0) {
            return new ProcessingMetrics(0.0, 0.0);
        }

        double averageProcessingHours = totalHours / measuredCount;
        double targetComplianceRate = (compliantCount * 100.0) / measuredCount;

        return new ProcessingMetrics(averageProcessingHours, targetComplianceRate);
    }

    private Map<String, Long> toStringKeyedMap(List<Object[]> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "INCONNU";
            result.put(key, (Long) row[1]);
        }
        return result;
    }

    private record ProcessingMetrics(double averageProcessingHours, double targetComplianceRate) {
    }
}