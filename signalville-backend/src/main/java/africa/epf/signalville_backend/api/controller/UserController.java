package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.request.AdminUpdateUserRequest;
import africa.epf.signalville_backend.api.dto.request.CreateInternalUserRequest;
import africa.epf.signalville_backend.api.dto.request.UpdateProfileRequest;
import africa.epf.signalville_backend.api.dto.request.UpdateUserStatusRequest;
import africa.epf.signalville_backend.api.dto.response.CreateInternalUserResponse;
import africa.epf.signalville_backend.api.dto.response.UserPage;
import africa.epf.signalville_backend.api.dto.response.UserResponse;
import africa.epf.signalville_backend.application.service.UserService;
import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.getById(principal.id()));
    }

    /**
     * Self-service : un utilisateur connecte modifie son propre profil
     * (prenom, nom, telephone). Ni role, ni statut, ni email ne transitent
     * par cet endpoint - voir UpdateProfileRequest.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal AppUserPrincipal principal,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.id(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserPage> search(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.search(role, status, search, pageable));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getById(userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<CreateInternalUserResponse> createInternalUser(
            @Valid @RequestBody CreateInternalUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createInternalUser(request));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponse> update(@PathVariable UUID userId,
                                                @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(userId, request));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable UUID userId,
                                                      @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.updateStatus(userId, request.status(), request.reason()));
    }
}