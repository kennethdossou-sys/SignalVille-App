package africa.epf.signalville_backend.api.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Tableau de bord du superviseur, exposé par GET /dashboard/supervisor.
 *
 * unassignedCount inclut NOUVEAU et REOUVERT (les deux sont "à affecter") ;
 * reopenedCount est un sous-ensemble affiché séparément comme signal
 * d'alerte. criticalCount/criticalReports filtrent sur Report.priority
 * (état effectif de l'instance), jamais sur Category.defaultPriority.
 * Décisions actées en Séance 4.
 */
public record SupervisorDashboardResponse(
        long unassignedCount,
        long reopenedCount,
        long last24HoursCount,
        long inProgressCount,
        long toVerifyCount,
        long closedCount,
        long criticalCount,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        Map<String, Long> byDistrict,
        List<ReportResponse> reportsToVerify,
        List<ReportResponse> criticalReports,
        List<ReportResponse> reopenedReports) {
}