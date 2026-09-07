package africa.epf.signalville_backend.application.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import africa.epf.signalville_backend.api.dto.request.AdminUpdateUserRequest;
import africa.epf.signalville_backend.api.dto.request.CreateInternalUserRequest;
import africa.epf.signalville_backend.api.dto.response.AvailableAgentResponse;
import africa.epf.signalville_backend.api.dto.response.CreateInternalUserResponse;
import africa.epf.signalville_backend.api.dto.response.UserPage;
import africa.epf.signalville_backend.api.dto.response.UserResponse;
import africa.epf.signalville_backend.application.mapper.UserMapper;
import africa.epf.signalville_backend.domain.exception.BusinessRuleException;
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

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TEMPORARY_PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final InterventionRepository interventionRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Utilisateur", id));
    }

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

    // ------------------------------------------------------------------
    // Module 3 — Administration
    // ------------------------------------------------------------------

    /** GET /users. Reserve a l'administrateur (verifie au niveau controleur). */
    @Transactional(readOnly = true)
    public UserPage search(Role role, AccountStatus status, String search, Pageable pageable) {
        Page<User> page = userRepository.search(role, status, search, pageable);
        return new UserPage(
                page.getContent().stream().map(UserMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * POST /users. Cree un compte interne (AGENT, SUPERVISEUR ou
     * ADMINISTRATEUR) avec un mot de passe temporaire genere aleatoirement,
     * jamais choisi par l'administrateur :
     * evite les mots de passe faibles ou dupliques qu'un choix humain repete
     * introduirait. Le mot de passe temporaire n'est retourne qu'une seule
     * fois, dans cette reponse precise — jamais stocke en clair, jamais
     * renvoye par une autre route. mustChangePassword est pose a true pour
     * signaler (sans imposer) le changement au frontend.
     */
   @Transactional
    public CreateInternalUserResponse createInternalUser(CreateInternalUserRequest request) {
        if (request.role() == Role.CITOYEN) {
            throw new BusinessRuleException("Un compte interne ne peut pas avoir le role CITOYEN");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Cette adresse email est deja utilisee");
        }

        String temporaryPassword = generateTemporaryPassword();

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(request.email().trim().toLowerCase())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .role(request.role())
                .status(AccountStatus.ACTIF)
                .mustChangePassword(true)
                .build();

        User saved = userRepository.save(user);
        return new CreateInternalUserResponse(UserMapper.toResponse(saved), temporaryPassword);
    }

    /** PUT /users/{userId}. Ne modifie jamais l'email (identifiant stable) ni le statut. */
    @Transactional
    public UserResponse update(UUID userId, AdminUpdateUserRequest request) {
        User user = requireUser(userId);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }

        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    /** PATCH /users/{userId}/status. Seul point d'entree pour changer le statut d'un compte. */
    @Transactional
    public UserResponse updateStatus(UUID userId, AccountStatus status, String reason) {
        User user = requireUser(userId);
        user.setStatus(status);
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Utilisateur", userId));
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int i = 0; i < TEMPORARY_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}