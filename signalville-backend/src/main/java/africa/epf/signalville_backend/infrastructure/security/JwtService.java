package africa.epf.signalville_backend.infrastructure.security;

import africa.epf.signalville_backend.domain.model.User;
import africa.epf.signalville_backend.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Emission et verification des access tokens (HS256).
 *
 * Le refresh token n'est volontairement pas un JWT : c'est un opaque aleatoire
 * persiste en base, ce qui permet de le revoquer au logout (strategie B).
 */
@Service
@Slf4j
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.accessTokenTtlSeconds());

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claims(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtlSeconds();
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(properties.refreshTokenTtlDays());
    }

    /** Retourne l'identifiant utilisateur si le token est valide, sinon vide. */
    public Optional<UUID> extractUserId(String token) {
        return parse(token).map(claims -> UUID.fromString(claims.getSubject()));
    }

    private Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token JWT rejete : {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
