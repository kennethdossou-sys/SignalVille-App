package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.response.AgentStatisticsResponse;
import africa.epf.signalville_backend.api.dto.response.StatisticsResponse;
import africa.epf.signalville_backend.api.dto.response.TimelinePointResponse;
import africa.epf.signalville_backend.application.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.time.LocalDate;
import java.util.List;

/**
 * GET /statistics/general, /statistics/agents, /statistics/timeline.
 * dateFrom/dateTo restent non exploités par getGeneralStatistics() (écart
 * connu avec le contrat, à traiter séparément — voir checkpoint de séance).
 */
@RestController
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/statistics/general")
    @PreAuthorize("hasAnyRole('SUPERVISEUR', 'ADMINISTRATEUR')")
    public StatisticsResponse getGeneralStatistics(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
    return statisticsService.getGeneralStatistics(dateFrom, dateTo);
    }

    @GetMapping("/statistics/agents")
    @PreAuthorize("hasAnyRole('SUPERVISEUR', 'ADMINISTRATEUR')")
    public List<AgentStatisticsResponse> getAgentStatistics() {
        return statisticsService.getAgentStatistics();
    }

    @GetMapping("/statistics/timeline")
    @PreAuthorize("hasAnyRole('SUPERVISEUR', 'ADMINISTRATEUR')")
    public List<TimelinePointResponse> getTimeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return statisticsService.getTimeline(dateFrom, dateTo, granularity);
    }
}