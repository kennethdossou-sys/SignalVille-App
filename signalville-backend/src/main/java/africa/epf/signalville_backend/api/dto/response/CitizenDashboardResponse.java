package africa.epf.signalville_backend.api.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Tableau de bord de l'espace citoyen, exposé par GET /dashboard/citizen.
 *
 * resolvedReports regroupe RESOLU et CLOTURE : du point de vue du citoyen,
 * son problème est réglé dès que l'agent a terminé, la vérification
 * superviseur est un détail de process interne (décision Séance 4).
 * openReports regroupe NOUVEAU, AFFECTE, EN_COURS, REOUVERT.
 */
public record CitizenDashboardResponse(
        long totalReports,
        Map<String, Long> byStatus,
        long openReports,
        long resolvedReports,
        long unreadNotifications,
        List<ReportResponse> recentReports) {
}