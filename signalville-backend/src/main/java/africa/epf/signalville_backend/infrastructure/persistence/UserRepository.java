package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Utilise pour GET /agents/available : agents actifs uniquement, tries par prenom. */
    List<User> findByRoleAndStatusOrderByFirstNameAsc(Role role, AccountStatus status);

    // ------------------------------------------------------------------
    // Module 3 — Administration (GET /users)
    // ------------------------------------------------------------------

    /**
     * Recherche paginee avec filtres optionnels, reservee a l'administrateur.
     * Meme pattern que ReportRepository.searchAll : filtres neutralises
     * lorsqu'ils valent null, evite une Specification pour un besoin simple.
     */
    @Query("""
            select u from User u
            where (:role   is null or u.role   = :role)
              and (:status is null or u.status = :status)
              and (cast(:search as string) is null
                   or lower(u.firstName) like lower(concat('%', cast(:search as string), '%'))
                   or lower(u.lastName)  like lower(concat('%', cast(:search as string), '%'))
                   or lower(u.email)     like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<User> search(@Param("role") Role role,
                       @Param("status") AccountStatus status,
                       @Param("search") String search,
                       Pageable pageable);

    /** Utilise par GET /dashboard/admin : repartition des comptes par role. */
    @Query("select u.role, count(u) from User u group by u.role")
    List<Object[]> countGroupedByRole();

    /** Utilise par GET /dashboard/admin : repartition des comptes par statut. */
    @Query("select u.status, count(u) from User u group by u.status")
    List<Object[]> countGroupedByStatus();
}