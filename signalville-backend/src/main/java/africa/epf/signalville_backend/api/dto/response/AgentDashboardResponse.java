package africa.epf.signalville_backend.api.dto.response;

import java.util.List;

/**
 * Tableau de bord de l'espace agent, exposé par GET /dashboard/agent.
 *
 * assignedCount (AFFECTEE, pas encore démarré) et inProgressCount (EN_COURS,
 * en cours de traitement) sont deux compteurs distincts, sans chevauchement
 * — contrairement à unassignedCount/reopenedCount côté superviseur.
 * resolvedCount est un cumul total (pas de fenêtre temporelle), cohérent
 * avec AgentStatisticsResponse.resolvedCount de GET /statistics/agents.
 * currentInterventions regroupe les dossiers actifs (AFFECTEE + EN_COURS).
 * Décisions actées en Séance 4.
 */
public record AgentDashboardResponse(
        long assignedCount,
        long inProgressCount,
        long resolvedCount,
        double averageProcessingHours,
        long unreadNotifications,
        List<ReportResponse> currentInterventions) {
}