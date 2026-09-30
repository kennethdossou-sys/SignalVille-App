package africa.epf.signalville_backend.application.service;

import java.util.List;
import java.util.UUID;

import africa.epf.signalville_backend.api.dto.request.CategoryRequest;
import africa.epf.signalville_backend.api.dto.response.CategoryResponse;

/**
 * Contrat du service de gestion des categories. L'implementation vit dans
 * application.service.impl.CategoryServiceImpl — les appelants (controleurs,
 * autres services) ne dependent que de cette interface, jamais de sa
 * realisation concrete (inversion de dependance).
 */
public interface CategoryService {

    /** GET /categories du contrat : uniquement les categories actives. */
    List<CategoryResponse> listActive();

    /** GET /categories/all. Reserve a l'administrateur. */
    List<CategoryResponse> listAll();

    /** GET /categories/{id}. Reserve a l'administrateur. */
    CategoryResponse getById(UUID categoryId);

    /**
     * POST /categories. Reserve a l'administrateur (verifie au niveau
     * controleur). Le nom doit etre unique, insensible a la casse.
     */
    CategoryResponse create(CategoryRequest request);

    /**
     * PUT /categories/{id}. Ne modifie jamais le statut actif — voir
     * CategoryMapper.applyUpdate pour la justification.
     */
    CategoryResponse update(UUID categoryId, CategoryRequest request);

    /** PATCH /categories/{id}/status. Seul point d'entree pour activer/desactiver. */
    void updateStatus(UUID categoryId, boolean active);
}