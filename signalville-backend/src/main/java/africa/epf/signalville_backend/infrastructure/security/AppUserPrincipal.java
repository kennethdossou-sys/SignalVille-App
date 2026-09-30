package africa.epf.signalville_backend.infrastructure.security;

import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Principal expose a Spring Security. Il porte l'identifiant technique de
 * l'utilisateur pour que les services puissent filtrer par proprietaire sans
 * relire la base a chaque requete.
 */
public record AppUserPrincipal(
        UUID id,
        String email,
        String passwordHash,
        Role role,
        AccountStatus status
) implements UserDetails {

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIF;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AccountStatus.SUSPENDU;
    }
}
