package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.AvailableAgentResponse;
import africa.epf.signalville_backend.api.dto.response.UserResponse;
import africa.epf.signalville_backend.application.mapper.UserMapper;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Intervention;
import africa.epf.signalville_backend.domain.model.Report;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.persistence.InterventionRepository;
import africa.epf.signalville_backend.infrastructure.persistence.ReportRepository;
import africa.epf.signalville_backend.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final InterventionRepository interventionRepository;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Utilisateur", id));
    }

    /**
     * Agents actifs disponibles pour affectation, avec leur charge courante
     * (nombre d'interventions en statut AFFECTEE ou EN_COURS). Reserve au
     * superviseur - permet d'equilibrer la charge plutot que d'affecter a
     * l'aveugle (besoin explicite du cadrage S3, section Affectation).
     *
     * Si reportId est fourni et que le signalement correspondant est au
     * statut REOUVERT, l'agent de la derniere intervention est exclu de la
     * liste : un dossier rouvert ne peut pas etre reaffecte au meme agent
     * (regle metier Seance 4). InterventionService.assign() applique la
     * meme regle cote validation, en garde-fou independant de ce filtrage
     * amont - les deux existent volontairement, comme la contrainte
     * d'unicite en base double deja le controle applicatif ailleurs dans
     * le Module 2.
     */
    @Transactional(readOnly = true)
    public List<AvailableAgentResponse> listAvailableAgents(UUID reportId) {
        List<User> agents = userRepository.findByRoleAndStatusOrderByFirstNameAsc(Role.AGENT, AccountStatus.ACTIF);

        UUID excludedAgentId = reportId == null ? null : findExcludedAgentId(reportId);

        return agents.stream()
                .filter(agent -> excludedAgentId == null || !agent.getId().equals(excludedAgentId))
                .map(agent -> new AvailableAgentResponse(
                        agent.getId(),
                        agent.getFirstName(),
                        agent.getLastName(),
                        agent.getEmail(),
                        agent.getStatus(),
                        interventionRepository.countActiveByAgentId(agent.getId())))
                .toList();
    }

    /**
     * Determine l'agent a exclure pour un signalement donne. Retourne null
     * si le signalement n'existe pas, n'est pas REOUVERT, ou n'a aucune
     * intervention anterieure - dans tous ces cas, aucune exclusion.
     */
    private UUID findExcludedAgentId(UUID reportId) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null || report.getStatus() != ReportStatus.REOUVERT) {
            return null;
        }
        Optional<Intervention> lastIntervention = interventionRepository
                .findByReportIdOrderByAssignedAtDesc(reportId)
                .stream()
                .findFirst();
        return lastIntervention.map(intervention -> intervention.getAgent().getId()).orElse(null);
    }
}