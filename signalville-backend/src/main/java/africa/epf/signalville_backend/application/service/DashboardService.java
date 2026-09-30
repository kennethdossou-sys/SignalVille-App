package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.AdminDashboardResponse;
import africa.epf.signalville_backend.api.dto.response.AgentDashboardResponse;
import africa.epf.signalville_backend.api.dto.response.CitizenDashboardResponse;
import africa.epf.signalville_backend.api.dto.response.SupervisorDashboardResponse;

import java.util.UUID;

/**
 * Contrat de construction des tableaux de bord par role : citoyen, agent,
 * superviseur, admin. L'implementation vit dans
 * application.service.impl.DashboardServiceImpl.
 */
public interface DashboardService {

    SupervisorDashboardResponse getSupervisorDashboard();

    CitizenDashboardResponse getCitizenDashboard(UUID citizenId);

    AgentDashboardResponse getAgentDashboard(UUID agentId);

    AdminDashboardResponse getAdminDashboard();
}