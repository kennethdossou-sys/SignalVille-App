package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.AgentStatisticsResponse;
import africa.epf.signalville_backend.api.dto.response.StatisticsResponse;
import africa.epf.signalville_backend.api.dto.response.TimelinePointResponse;
import africa.epf.signalville_backend.domain.exception.BusinessRuleException;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.infrastructure.persistence.InterventionRepository;
import africa.epf.signalville_backend.infrastructure.persistence.ReportRepository;
import africa.epf.signalville_backend.infrastructure.persistence.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Calcule les statistiques de la plateforme : générales (GET /statistics/general),
 * par agent (GET /statistics/agents) et temporelles (GET /statistics/timeline).
 *
 * Décisions actées en Séance 4 :
 * - averageProcessingHours (général) : StatusHistory, dernière transition
 *   vers CLOTURE, jamais Report.updatedAt (donnée technique modifiable).
 * - averageProcessingHours (par agent) : resolvedAt - startedAt (temps de
 *   travail effectif), pas assignedAt - resolvedAt (délai total).
 * - reassignedCount (par agent) : interventions de cet agent passées à
 *   REAFFECTEE (dossiers dessaisis), pas les dossiers reçus.
 * - GET /statistics/agents n'affiche que les agents ayant ≥ 1 intervention :
 *   leur absence communique déjà l'information pour les autres.
 * - GET /statistics/timeline : fenêtre par défaut de 30 jours si dateFrom/
 *   dateTo absents ; resolvedCount compte les transitions vers RESOLU, pas
 *   CLOTURE ; buckets continus (zéro-remplis) pour un graphique sans trous.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private static final Set<String> VALID_GRANULARITIES = Set.of("DAY", "WEEK", "MONTH");
    private static final int DEFAULT_TIMELINE_WINDOW_DAYS = 30;

    private final ReportRepository reportRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final InterventionRepository interventionRepository;

    // ------------------------------------------------------------------
    // GET /statistics/general
    // ------------------------------------------------------------------

    public StatisticsResponse getGeneralStatistics(LocalDate dateFrom, LocalDate dateTo) {
    LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
    LocalDateTime toExclusive = dateTo != null ? dateTo.plusDays(1).atStartOfDay() : null;

    long totalReports = reportRepository.countInRange(from, toExclusive);

    Map<String, Long> byStatus = toStringKeyedMap(reportRepository.countGroupedByStatusInRange(from, toExclusive));
    Map<String, Long> byCategory = toStringKeyedMap(reportRepository.countGroupedByCategoryInRange(from, toExclusive));
    Map<String, Long> byDistrict = toStringKeyedMap(reportRepository.countGroupedByDistrictInRange(from, toExclusive));

    ProcessingMetrics metrics = computeProcessingMetrics(from, toExclusive);

    return new StatisticsResponse(
            totalReports,
            byStatus,
            byCategory,
            byDistrict,
            metrics.averageProcessingHours(),
            metrics.targetComplianceRate());
}

    private ProcessingMetrics computeProcessingMetrics(LocalDateTime from, LocalDateTime toExclusive) {
    List<Report> closedReports = reportRepository
            .searchAll(ReportStatus.CLOTURE, null, null, null, Pageable.unpaged())
            .getContent();

    if (closedReports.isEmpty()) {
        return new ProcessingMetrics(0.0, 0.0);
    }

    Map<UUID, LocalDateTime> lastClosedAtByReportId = new HashMap<>();
    for (Object[] row : statusHistoryRepository.findLastClosedAtByReportId()) {
        lastClosedAtByReportId.put((UUID) row[0], (LocalDateTime) row[1]);
    }

    double totalHours = 0.0;
    long compliantCount = 0;
    long measuredCount = 0;

    for (Report report : closedReports) {
        // Filtre par date de creation, coherent avec les autres agregats de
        // cette reponse — dateFrom/dateTo absents = aucun filtrage (comportement historique).
        if (from != null && report.getCreatedAt().isBefore(from)) {
            continue;
        }
        if (toExclusive != null && !report.getCreatedAt().isBefore(toExclusive)) {
            continue;
        }

        LocalDateTime lastClosedAt = lastClosedAtByReportId.get(report.getId());
        if (lastClosedAt == null) {
            continue;
        }

        long hours = ChronoUnit.HOURS.between(report.getCreatedAt(), lastClosedAt);
        totalHours += hours;
        measuredCount++;

        Integer targetDelayHours = report.getCategory().getTargetDelayHours();
        if (targetDelayHours != null && hours <= targetDelayHours) {
            compliantCount++;
        }
    }

    if (measuredCount == 0) {
        return new ProcessingMetrics(0.0, 0.0);
    }

    return new ProcessingMetrics(totalHours / measuredCount, (compliantCount * 100.0) / measuredCount);
}

    // ------------------------------------------------------------------
    // GET /statistics/agents
    // ------------------------------------------------------------------

    public List<AgentStatisticsResponse> getAgentStatistics() {
        List<Object[]> assignedRows = interventionRepository.countAssignedGroupedByAgent();
        Map<UUID, Long> resolvedByAgent = toUuidLongMap(interventionRepository.countResolvedGroupedByAgent());
        Map<UUID, Long> reassignedByAgent = toUuidLongMap(interventionRepository.countReassignedGroupedByAgent());
        Map<UUID, Double> avgHoursByAgent = toUuidDoubleMap(interventionRepository.averageProcessingHoursGroupedByAgent());

        List<AgentStatisticsResponse> result = new ArrayList<>();
        for (Object[] row : assignedRows) {
            UUID agentId = (UUID) row[0];
            String fullName = row[1] + " " + row[2];
            long assignedCount = (Long) row[3];

            result.add(new AgentStatisticsResponse(
                    agentId,
                    fullName,
                    assignedCount,
                    resolvedByAgent.getOrDefault(agentId, 0L),
                    reassignedByAgent.getOrDefault(agentId, 0L),
                    avgHoursByAgent.getOrDefault(agentId, 0.0)));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // GET /statistics/timeline
    // ------------------------------------------------------------------

    public List<TimelinePointResponse> getTimeline(LocalDate dateFrom, LocalDate dateTo, String granularityParam) {
        String granularity = normalizeGranularity(granularityParam);

        LocalDate effectiveFrom = dateFrom != null ? dateFrom : LocalDate.now().minusDays(DEFAULT_TIMELINE_WINDOW_DAYS);
        LocalDate effectiveTo = dateTo != null ? dateTo : LocalDate.now();

        LocalDateTime fromDateTime = effectiveFrom.atStartOfDay();
        LocalDateTime toExclusive = effectiveTo.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> createdByPeriod = toDateLongMap(
                reportRepository.countCreatedGroupedByPeriod(granularity, fromDateTime, toExclusive));
        Map<LocalDate, Long> resolvedByPeriod = toDateLongMap(
                statusHistoryRepository.countResolvedGroupedByPeriod(granularity, fromDateTime, toExclusive));

        return generateBuckets(effectiveFrom, effectiveTo, granularity).stream()
                .map(bucket -> new TimelinePointResponse(
                        bucket,
                        createdByPeriod.getOrDefault(bucket, 0L),
                        resolvedByPeriod.getOrDefault(bucket, 0L)))
                .toList();
    }

    private String normalizeGranularity(String granularityParam) {
        String upper = granularityParam == null ? "DAY" : granularityParam.toUpperCase();
        if (!VALID_GRANULARITIES.contains(upper)) {
            throw new BusinessRuleException(
                    "Granularite invalide : " + granularityParam + " (attendu : DAY, WEEK ou MONTH)");
        }
        return upper.toLowerCase();
    }

    private LocalDate truncateToBucketStart(LocalDate date, String granularity) {
        return switch (granularity) {
            case "week" -> date.with(DayOfWeek.MONDAY);
            case "month" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    private List<LocalDate> generateBuckets(LocalDate from, LocalDate to, String granularity) {
        LocalDate cursor = truncateToBucketStart(from, granularity);
        LocalDate lastBucket = truncateToBucketStart(to, granularity);

        List<LocalDate> buckets = new ArrayList<>();
        while (!cursor.isAfter(lastBucket)) {
            buckets.add(cursor);
            cursor = switch (granularity) {
                case "week" -> cursor.plusWeeks(1);
                case "month" -> cursor.plusMonths(1);
                default -> cursor.plusDays(1);
            };
        }
        return buckets;
    }

    // --- helpers de conversion ---

    private Map<String, Long> toStringKeyedMap(List<Object[]> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(row[0] != null ? row[0].toString() : "INCONNU", (Long) row[1]);
        }
        return result;
    }

    private Map<UUID, Long> toUuidLongMap(List<Object[]> rows) {
        Map<UUID, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], (Long) row[1]);
        }
        return result;
    }

    private Map<UUID, Double> toUuidDoubleMap(List<Object[]> rows) {
        Map<UUID, Double> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], (Double) row[1]);
        }
        return result;
    }

    private Map<LocalDate, Long> toDateLongMap(List<Object[]> rows) {
        Map<LocalDate, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(toLocalDate(row[0]), (Long) row[1]);
        }
        return result;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.toLocalDate();
        }
        throw new IllegalStateException("Type de date inattendu pour un bucket timeline : " + value.getClass());
    }

    private record ProcessingMetrics(double averageProcessingHours, double targetComplianceRate) {
    }
}