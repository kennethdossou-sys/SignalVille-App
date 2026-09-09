package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.AgentDashboardResponse;
import africa.epf.signalville_backend.api.dto.response.CitizenDashboardResponse;
import africa.epf.signalville_backend.api.dto.response.ReportResponse;
import africa.epf.signalville_backend.api.dto.response.SupervisorDashboardResponse;
import africa.epf.signalville_backend.api.dto.response.AdminDashboardResponse;
import africa.epf.signalville_backend.application.mapper.ReportMapper;
import africa.epf.signalville_backend.domain.model.Intervention;
import africa.epf.signalville_backend.domain.model.InterventionStatus;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.infrastructure.persistence.InterventionRepository;
import africa.epf.signalville_backend.infrastructure.persistence.NotificationRepository;
import africa.epf.signalville_backend.infrastructure.persistence.ReportRepository;
import africa.epf.signalville_backend.infrastructure.persistence.CategoryRepository;
import africa.epf.signalville_backend.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Construit les tableaux de bord par rôle : citoyen, agent, superviseur, admin.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int SHORT_LIST_SIZE = 10;

    private static final List<ReportStatus> OPEN_STATUSES = List.of(
            ReportStatus.NOUVEAU, ReportStatus.AFFECTE, ReportStatus.EN_COURS, ReportStatus.REOUVERT);

    private static final List<ReportStatus> RESOLVED_STATUSES = List.of(
            ReportStatus.RESOLU, ReportStatus.CLOTURE);

    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final InterventionRepository interventionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public SupervisorDashboardResponse getSupervisorDashboard() {
        List<Report> newReports = reportRepository.findByStatusOrderByUpdatedAtAsc(ReportStatus.NOUVEAU);
        List<Report> reopenedList = reportRepository.findByStatusOrderByUpdatedAtAsc(ReportStatus.REOUVERT);

        long newCount = newReports.size();
        long reopenedCount = reopenedList.size();
        long unassignedCount = newCount + reopenedCount;

        List<Report> unassigned = new ArrayList<>(newReports);
        unassigned.addAll(reopenedList);

        long last24HoursCount = reportRepository.countCreatedSince(LocalDateTime.now().minusHours(24));

        long inProgressCount = reportRepository.countByStatus(ReportStatus.AFFECTE)
                + reportRepository.countByStatus(ReportStatus.EN_COURS);

        long closedCount = reportRepository.countByStatus(ReportStatus.CLOTURE);

        List<Report> toVerify = reportRepository.findByStatusOrderByUpdatedAtAsc(ReportStatus.RESOLU);
        List<Report> critical = reportRepository.findCriticalActive();

        Map<String, Long> byStatus = toStringKeyedMap(reportRepository.countGroupedByStatus());
        Map<String, Long> byCategory = toStringKeyedMap(reportRepository.countGroupedByCategory());
        Map<String, Long> byDistrict = toStringKeyedMap(reportRepository.countGroupedByDistrict());

        return new SupervisorDashboardResponse(
                unassignedCount,
                reopenedCount,
                last24HoursCount,
                inProgressCount,
                (long) toVerify.size(),
                closedCount,
                (long) critical.size(),
                byStatus,
                byCategory,
                byDistrict,
                toShortList(toVerify),
                toShortList(critical),
                toShortList(reopenedList),
                toShortList(unassigned));
    }

    public CitizenDashboardResponse getCitizenDashboard(UUID citizenId) {
        long totalReports = reportRepository.countByCitizenIdAndStatusIn(citizenId, List.of(ReportStatus.values()));
        long openReports = reportRepository.countByCitizenIdAndStatusIn(citizenId, OPEN_STATUSES);
        long resolvedReports = reportRepository.countByCitizenIdAndStatusIn(citizenId, RESOLVED_STATUSES);
        long unreadNotifications = notificationRepository.countByRecipientIdAndReadFalse(citizenId);

        List<Report> recent = reportRepository.findByCitizenIdOrderByCreatedAtDesc(
                citizenId, PageRequest.of(0, SHORT_LIST_SIZE));

        Map<String, Long> byStatus = toStringKeyedMap(reportRepository.countGroupedByStatusForCitizen(citizenId));

        List<ReportResponse> recentReports = recent.stream().map(ReportMapper::toResponse).toList();

        return new CitizenDashboardResponse(
                totalReports,
                byStatus,
                openReports,
                resolvedReports,
                unreadNotifications,
                recentReports);
    }

    public AgentDashboardResponse getAgentDashboard(UUID agentId) {
        List<Intervention> active = interventionRepository.findActiveByAgentId(agentId);

        long assignedCount = active.stream()
                .filter(i -> i.getStatus() == InterventionStatus.AFFECTEE)
                .count();
        long inProgressCount = active.stream()
                .filter(i -> i.getStatus() == InterventionStatus.EN_COURS)
                .count();

        long unreadNotifications = notificationRepository.countByRecipientIdAndReadFalse(agentId);

        AgentResolvedMetrics metrics = computeAgentResolvedMetrics(agentId);

        List<ReportResponse> currentInterventions = active.stream()
                .map(Intervention::getReport)
                .map(ReportMapper::toResponse)
                .toList();

        return new AgentDashboardResponse(
                assignedCount,
                inProgressCount,
                metrics.resolvedCount(),
                metrics.averageProcessingHours(),
                unreadNotifications,
                currentInterventions);
    }

    private AgentResolvedMetrics computeAgentResolvedMetrics(UUID agentId) {
        long resolvedCount = interventionRepository.countResolvedGroupedByAgent().stream()
                .filter(row -> agentId.equals(row[0]))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);

        // Number.doubleValue() gere BigDecimal (type reel retourne par AVG()
        // PostgreSQL) sans dependre du typage exact renvoye par le driver JDBC.
        double averageProcessingHours = interventionRepository.averageProcessingHoursGroupedByAgent().stream()
                .filter(row -> agentId.equals(row[0]))
                .map(row -> ((Number) row[1]).doubleValue())
                .findFirst()
                .orElse(0.0);

        return new AgentResolvedMetrics(resolvedCount, averageProcessingHours);
    }

    private List<ReportResponse> toShortList(List<Report> reports) {
        return reports.stream()
                .limit(SHORT_LIST_SIZE)
                .map(ReportMapper::toResponse)
                .toList();
    }

    private Map<String, Long> toStringKeyedMap(List<Object[]> rows) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(row[0] != null ? row[0].toString() : "INCONNU", (Long) row[1]);
        }
        return result;
    }

    private record AgentResolvedMetrics(long resolvedCount, double averageProcessingHours) {
    }

    public AdminDashboardResponse getAdminDashboard() {
        long totalUsers = userRepository.count();
        Map<String, Long> usersByRole = toStringKeyedMap(userRepository.countGroupedByRole());
        Map<String, Long> usersByStatus = toStringKeyedMap(userRepository.countGroupedByStatus());

        long totalCategories = categoryRepository.count();
        long activeCategories = categoryRepository.countByActiveTrue();

        long totalReports = reportRepository.count();
        long reportsLast30Days = reportRepository.countCreatedSince(LocalDateTime.now().minusDays(30));

        return new AdminDashboardResponse(
                totalUsers,
                usersByRole,
                usersByStatus,
                totalCategories,
                activeCategories,
                totalReports,
                reportsLast30Days);
    }
}