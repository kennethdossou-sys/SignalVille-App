package africa.epf.signalville_backend.api.dto.response;

import java.util.Map;

/**
 * Tableau de bord administrateur, exposé par GET /dashboard/admin.
 * Vue d'ensemble de la plateforme (comptes, catégories, volume), sans
 * détail actionnable comme les autres dashboards — l'administration
 * elle-même se fait via GET/POST /users et /categories, pas ici.
 */
public record AdminDashboardResponse(
        long totalUsers,
        Map<String, Long> usersByRole,
        Map<String, Long> usersByStatus,
        long totalCategories,
        long activeCategories,
        long totalReports,
        long reportsLast30Days
) {
}