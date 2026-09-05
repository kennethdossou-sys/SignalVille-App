package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.response.StatisticsResponse;
import africa.epf.signalville_backend.application.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /statistics/general uniquement dans cette tranche. dateFrom/dateTo
 * (paramètres contractualisés) et /statistics/agents, /statistics/timeline
 * restent hors périmètre — non implémentés, pas de faux-semblant de filtrage.
 */
@RestController
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/statistics/general")
    @PreAuthorize("hasAnyRole('SUPERVISEUR', 'ADMINISTRATEUR')")
    public StatisticsResponse getGeneralStatistics() {
        return statisticsService.getGeneralStatistics();
    }
}