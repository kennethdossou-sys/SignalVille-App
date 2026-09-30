package africa.epf.signalville_backend.application.mapper;

import africa.epf.signalville_backend.api.dto.request.CategoryRequest;
import africa.epf.signalville_backend.api.dto.response.CategoryResponse;
import africa.epf.signalville_backend.domain.model.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getDefaultPriority(),
                category.getTargetDelayHours(),
                category.isActive(),
                category.getCreatedAt());
    }

    /** Construction d'une nouvelle Category a partir d'une requete de creation (POST). */
    public static Category toEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.name().trim())
                .description(request.description())
                .icon(request.icon())
                .defaultPriority(request.defaultPriority())
                .targetDelayHours(request.targetDelayHours())
                .active(request.activeOrDefault())
                .build();
    }

    /**
     * Applique une requete de modification (PUT) sur une Category existante,
     * en place. Le statut actif n'est volontairement pas touche ici : il est
     * gere exclusivement par PATCH /categories/{id}/status, endpoint dedie,
     * pour eviter qu'une simple modification de nom desactive une categorie
     * par erreur (defaut du DTO non renseigne).
     */
    public static void applyUpdate(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setIcon(request.icon());
        category.setDefaultPriority(request.defaultPriority());
        category.setTargetDelayHours(request.targetDelayHours());
    }
}