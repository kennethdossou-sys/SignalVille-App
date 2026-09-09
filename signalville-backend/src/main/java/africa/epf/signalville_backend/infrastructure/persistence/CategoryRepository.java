package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    /**
     * Utilise pour valider l'unicite du nom lors d'une MODIFICATION (PUT) :
     * exclut la categorie elle-meme de la verification, sinon elle se
     * bloquerait toujours elle-meme (son propre nom existe deja en base).
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** Utilise par GET /dashboard/admin : nombre de categories actives. */
    long countByActiveTrue();

        /**
     * Toutes les categories, actives et inactives, triees par nom.
     * Reserve a GET /categories/all (administration) — GET /categories
     * (public) reste limite aux actives via findByActiveTrueOrderByNameAsc.
     */
    List<Category> findAllByOrderByNameAsc();
}