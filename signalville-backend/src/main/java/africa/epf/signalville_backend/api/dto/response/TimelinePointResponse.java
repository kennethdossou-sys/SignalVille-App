package africa.epf.signalville_backend.api.dto.response;

import java.time.LocalDate;

/**
 * Point unique d'une série temporelle de signalements (GET /statistics/timeline).
 * createdCount compte les créations sur la période ; resolvedCount compte les
 * transitions vers RESOLU (pas CLOTURE) — décision Séance 4.
 */
public record TimelinePointResponse(
        LocalDate date,
        long createdCount,
        long resolvedCount) {
}