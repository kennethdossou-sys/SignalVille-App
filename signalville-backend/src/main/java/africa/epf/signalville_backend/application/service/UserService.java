package africa.epf.signalville_backend.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import africa.epf.signalville_backend.api.dto.request.AdminUpdateUserRequest;
import africa.epf.signalville_backend.api.dto.request.CreateInternalUserRequest;
import africa.epf.signalville_backend.api.dto.request.UpdateProfileRequest;
import africa.epf.signalville_backend.api.dto.response.AvailableAgentResponse;
import africa.epf.signalville_backend.api.dto.response.CreateInternalUserResponse;
import africa.epf.signalville_backend.api.dto.response.UserPage;
import africa.epf.signalville_backend.api.dto.response.UserResponse;
import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Role;

/**
 * Contrat du service de gestion des utilisateurs. L'implementation vit dans
 * application.service.impl.UserServiceImpl.
 */
public interface UserService {

    UserResponse getById(UUID id);

    List<AvailableAgentResponse> listAvailableAgents(UUID reportId);

    /** GET /users. Reserve a l'administrateur (verifie au niveau controleur). */
    UserPage search(Role role, AccountStatus status, String search, Pageable pageable);

    /**
     * POST /users. Cree un compte interne (AGENT, SUPERVISEUR ou
     * ADMINISTRATEUR) avec un mot de passe temporaire genere aleatoirement,
     * jamais choisi par l'administrateur.
     */
    CreateInternalUserResponse createInternalUser(CreateInternalUserRequest request);

    /** PUT /users/{userId}. Ne modifie jamais l'email (identifiant stable) ni le statut. */
    UserResponse update(UUID userId, AdminUpdateUserRequest request);

    /** PATCH /users/{userId}/status. Seul point d'entree pour changer le statut d'un compte. */
    UserResponse updateStatus(UUID userId, AccountStatus status, String reason);

    /**
     * Self-service : l'utilisateur connecte met a jour son propre profil
     * (prenom, nom, telephone). Ni role, ni statut, ni email ne transitent
     * par cette methode - voir UpdateProfileRequest.
     */
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
}