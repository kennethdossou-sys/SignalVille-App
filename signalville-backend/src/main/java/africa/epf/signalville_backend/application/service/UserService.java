package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.AvailableAgentResponse;
import africa.epf.signalville_backend.api.dto.response.UserResponse;
import africa.epf.signalville_backend.application.mapper.UserMapper;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.persistence.InterventionRepository;
import africa.epf.signalville_backend.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final InterventionRepository interventionRepository;

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
     */
    @Transactional(readOnly = true)
    public List<AvailableAgentResponse> listAvailableAgents() {
        List<User> agents = userRepository.findByRoleAndStatusOrderByFirstNameAsc(Role.AGENT, AccountStatus.ACTIF);
        return agents.stream()
                .map(agent -> new AvailableAgentResponse(
                        agent.getId(),
                        agent.getFirstName(),
                        agent.getLastName(),
                        agent.getEmail(),
                        agent.getStatus(),
                        interventionRepository.countActiveByAgentId(agent.getId())))
                .toList();
    }
}