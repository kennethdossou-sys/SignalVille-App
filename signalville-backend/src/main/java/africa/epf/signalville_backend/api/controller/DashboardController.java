package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.response.SupervisorDashboardResponse;
import africa.epf.signalville_backend.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /dashboard/supervisor uniquement dans cette tranche. /dashboard/citizen,
 * /dashboard/agent, /dashboard/admin restent hors périmètre (voir
 * DashboardService pour le détail des dépendances manquantes : Notification,
 * Intervention et User repositories respectivement).
 */
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/supervisor")
    @PreAuthorize("hasRole('SUPERVISEUR')")
    public SupervisorDashboardResponse getSupervisorDashboard() {
        return dashboardService.getSupervisorDashboard();
    }
}