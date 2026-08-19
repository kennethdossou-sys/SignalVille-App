package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.response.AvailableAgentResponse;
import africa.epf.signalville_backend.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint dedie au besoin metier du superviseur : choisir un agent pour
 * affecter un signalement. Volontairement separe de UserController, qui
 * reste sous la responsabilite de la gestion generale des comptes
 * (ADMINISTRATEUR). Aucune donnee de gestion de compte n'est exposee ici.
 */
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final UserService userService;

    @GetMapping("/available")
    @PreAuthorize("hasRole('SUPERVISEUR')")
    public ResponseEntity<List<AvailableAgentResponse>> listAvailable() {
        return ResponseEntity.ok(userService.listAvailableAgents());
    }
}