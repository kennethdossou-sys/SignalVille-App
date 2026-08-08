package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.request.LoginRequest;
import africa.epf.signalville_backend.api.dto.request.RegisterRequest;
import africa.epf.signalville_backend.api.dto.response.AuthResponse;
import africa.epf.signalville_backend.application.mapper.UserMapper;
import africa.epf.signalville_backend.domain.exception.EmailAlreadyUsedException;
import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.RefreshToken;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.persistence.RefreshTokenRepository;
import africa.epf.signalville_backend.infrastructure.persistence.UserRepository;
import africa.epf.signalville_backend.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Cas d'usage d'authentification (strategie JWT B).
 *
 * L'access token est un JWT signe, non revocable, de duree courte.
 * Le refresh token est un secret opaque persiste : il est revocable, et il est
 * remplace a chaque rafraichissement (rotation) pour limiter la fenetre de rejeu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 48;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(request.email().trim().toLowerCase())
                .phone(request.phone().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                // L'inscription publique ne cree que des citoyens. Les comptes internes
                // passent par POST /users, reserve aux administrateurs.
                .role(Role.CITOYEN)
                .status(AccountStatus.ACTIF)
                .build();

        User saved = userRepository.save(user);
        log.info("Nouveau citoyen inscrit : {}", saved.getEmail());
        return issueTokens(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        if (user.getStatus() != AccountStatus.ACTIF) {
            throw new DisabledException("Compte " + user.getStatus().name().toLowerCase());
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Une seule session active a la fois : les refresh precedents sont revoques.
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String presentedToken) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository.findByToken(presentedToken)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inconnu"));

        if (!stored.isUsable(now)) {
            throw new BadCredentialsException("Refresh token expire ou revoque");
        }

        User user = stored.getUser();
        if (user.getStatus() != AccountStatus.ACTIF) {
            throw new DisabledException("Compte " + user.getStatus().name().toLowerCase());
        }

        // Rotation : l'ancien token devient inutilisable des qu'un nouveau est emis.
        stored.setRevokedAt(now);
        refreshTokenRepository.save(stored);

        return issueTokens(user);
    }

    @Transactional
    public void logout(String presentedToken) {
        refreshTokenRepository.findByToken(presentedToken).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
        // Un token deja inconnu ou deja revoque donne quand meme 204 : le logout est idempotent.
    }

    @Transactional(readOnly = true)
    public User requireUserById(java.util.UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + id));
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = generateOpaqueToken();
        Instant now = Instant.now();

        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .issuedAt(now)
                .expiresAt(now.plus(jwtService.refreshTokenTtl()))
                .build());

        return AuthResponse.of(accessToken, refreshToken,
                jwtService.accessTokenTtlSeconds(), UserMapper.toResponse(user));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
