package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.AgentStatisticsResponse;
import africa.epf.signalville_backend.api.dto.response.StatisticsResponse;
import africa.epf.signalville_backend.api.dto.response.TimelinePointResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrat des statistiques de la plateforme : generales, par agent et
 * temporelles. L'implementation vit dans
 * application.service.impl.StatisticsServiceImpl.
 */
public interface StatisticsService {

    /** GET /statistics/general. dateFrom/dateTo optionnels — absence = aucun filtrage. */
    StatisticsResponse getGeneralStatistics(LocalDate dateFrom, LocalDate dateTo);

    /** GET /statistics/agents. N'affiche que les agents ayant au moins une intervention. */
    List<AgentStatisticsResponse> getAgentStatistics();

    /**
     * GET /statistics/timeline. Fenetre par defaut de 30 jours si dateFrom/
     * dateTo absents ; granularityParam attendu : DAY, WEEK ou MONTH.
     */
    List<TimelinePointResponse> getTimeline(LocalDate dateFrom, LocalDate dateTo, String granularityParam);
}