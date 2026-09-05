package africa.epf.signalville_backend.api.dto.response;

import java.util.UUID;

/**
 * Performance d'un agent sur les interventions qui lui ont été affectées.
 * N'apparaît dans GET /statistics/agents que si l'agent a au moins une
 * intervention (les agents sans activité sont absents plutôt qu'affichés
 * à zéro — leur absence communique déjà l'information).
 *
 * averageProcessingHours = resolvedAt - startedAt (temps de travail effectif
 * une fois l'intervention démarrée), calculé uniquement sur les interventions
 * RESOLUE. reassignedCount compte les interventions de cet agent passées au
 * statut REAFFECTEE (dossiers qui lui ont été retirés).
 */
public record AgentStatisticsResponse(
        UUID agentId,
        String agentName,
        long assignedCount,
        long resolvedCount,
        long reassignedCount,
        double averageProcessingHours) {
}