package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.response.AgentDashboardResponse;
import africa.epf.signalville_backend.api.dto.response.CitizenDashboardResponse;
import africa.epf.signalville_backend.api.dto.response.SupervisorDashboardResponse;
import africa.epf.signalville_backend.application.service.DashboardService;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /dashboard/supervisor, /dashboard/citizen, /dashboard/agent.
 * /dashboard/admin reste hors périmètre de ce lot.
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

    @GetMapping("/dashboard/citizen")
    @PreAuthorize("hasRole('CITOYEN')")
    public CitizenDashboardResponse getCitizenDashboard(@AuthenticationPrincipal AppUserPrincipal principal) {
        return dashboardService.getCitizenDashboard(principal.id());
    }

    @GetMapping("/dashboard/agent")
    @PreAuthorize("hasRole('AGENT')")
    public AgentDashboardResponse getAgentDashboard(@AuthenticationPrincipal AppUserPrincipal principal) {
        return dashboardService.getAgentDashboard(principal.id());
    }
}