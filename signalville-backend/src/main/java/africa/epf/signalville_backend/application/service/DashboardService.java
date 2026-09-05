package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.ReportResponse;
import africa.epf.signalville_backend.api.dto.response.SupervisorDashboardResponse;
import africa.epf.signalville_backend.application.mapper.ReportMapper;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.infrastructure.persistence.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit les tableaux de bord par rôle.
 *
 * Périmètre de cette tranche (Séance 4, chantier statistiques + dashboard
 * superviseur) : uniquement getSupervisorDashboard(). Les dashboards citoyen,
 * agent et administrateur nécessitent respectivement NotificationRepository,
 * InterventionRepository (interventions en cours de l'agent) et UserRepository
 * (comptage par rôle/statut) — non couverts ici, à traiter dans une tranche
 * ultérieure une fois ces dépôts confirmés.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    /**
     * Nombre de dossiers affichés dans les listes courtes du dashboard
     * (reportsToVerify, criticalReports, reopenedReports). Pas de pagination
     * sur ces widgets : ce sont des indicateurs de pilotage, pas des listes
     * de travail exhaustives (celles-ci restent accessibles via GET /reports,
     * paginé).
     */
    private static final int SHORT_LIST_SIZE = 10;

    private final ReportRepository reportRepository;

    public SupervisorDashboardResponse getSupervisorDashboard() {
        long newCount = reportRepository.countByStatus(ReportStatus.NOUVEAU);
        long reopenedCount = reportRepository.countByStatus(ReportStatus.REOUVERT);

        // unassignedCount = NOUVEAU + REOUVERT : les deux sont "à affecter",
        // reopenedCount reste par ailleurs affiché séparément comme signal
        // d'alerte (decision validee en Seance 4).
        long unassignedCount = newCount + reopenedCount;

        long last24HoursCount = reportRepository.countCreatedSince(LocalDateTime.now().minusHours(24));

        // Strictement AFFECTE + EN_COURS, conformement a la description du
        // contrat. REOUVERT est deja compte dans unassignedCount/reopenedCount.
        long inProgressCount = reportRepository.countByStatus(ReportStatus.AFFECTE)
                + reportRepository.countByStatus(ReportStatus.EN_COURS);

        long closedCount = reportRepository.countByStatus(ReportStatus.CLOTURE);

        List<Report> toVerify = reportRepository.findByStatusOrderByUpdatedAtAsc(ReportStatus.RESOLU);
        List<Report> critical = reportRepository.findCriticalActive();
        List<Report> reopened = reportRepository.findByStatusOrderByUpdatedAtAsc(ReportStatus.REOUVERT);

        Map<String, Long> byStatus = toStringKeyedMap(reportRepository.countGroupedByStatus());
        Map<String, Long> byCategory = toStringKeyedMap(reportRepository.countGroupedByCategory());
        Map<String, Long> byDistrict = toStringKeyedMap(reportRepository.countGroupedByDistrict());

        List<ReportResponse> reportsToVerify = toShortList(toVerify);
        List<ReportResponse> criticalReports = toShortList(critical);
        List<ReportResponse> reopenedReports = toShortList(reopened);

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
                reportsToVerify,
                criticalReports,
                reopenedReports);
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
            String key = row[0] != null ? row[0].toString() : "INCONNU";
            result.put(key, (Long) row[1]);
        }
        return result;
    }
}