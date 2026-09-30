package africa.epf.signalville_backend.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Acces au principal courant sans injecter le SecurityContext partout. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AppUserPrincipal> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static AppUserPrincipal require() {
        return get().orElseThrow(() -> new IllegalStateException("Aucun utilisateur authentifie dans le contexte"));
    }
}
