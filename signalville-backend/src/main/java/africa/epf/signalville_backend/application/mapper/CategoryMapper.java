package africa.epf.signalville_backend.application.mapper;

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
}
