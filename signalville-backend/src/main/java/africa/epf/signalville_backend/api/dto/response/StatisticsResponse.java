package africa.epf.signalville_backend.api.dto.response;

import java.util.Map;

/**
 * Statistiques générales de la plateforme, exposées par GET /statistics/general.
 *
 * averageProcessingHours et targetComplianceRate se basent sur la date réelle
 * de transition vers CLOTURE (StatusHistory.findLastClosedAtByReportId),
 * jamais sur Report.updatedAt — donnée technique modifiable après la clôture
 * qui fausserait silencieusement les statistiques historiques (Option B,
 * décision actée en Séance 4).
 */
public record StatisticsResponse(
        long totalReports,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        Map<String, Long> byDistrict,
        double averageProcessingHours,
        double targetComplianceRate) {
}