package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.request.CategoryRequest;
import africa.epf.signalville_backend.api.dto.response.CategoryResponse;
import africa.epf.signalville_backend.application.mapper.CategoryMapper;
import africa.epf.signalville_backend.domain.exception.BusinessRuleException;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.Category;
import africa.epf.signalville_backend.infrastructure.persistence.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** GET /categories du contrat : uniquement les categories actives. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    /** GET /categories/all. Reserve a l'administrateur. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    /**
     * POST /categories. Reserve a l'administrateur (verifie au niveau
     * controleur). Le nom doit etre unique, insensible a la casse.
     */
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        requireUniqueName(request.name(), null);
        Category saved = categoryRepository.save(CategoryMapper.toEntity(request));
        return CategoryMapper.toResponse(saved);
    }

    /**
     * PUT /categories/{id}. Ne modifie jamais le statut actif — voir
     * CategoryMapper.applyUpdate pour la justification.
     */
    @Transactional
    public CategoryResponse update(UUID categoryId, CategoryRequest request) {
        Category category = requireCategory(categoryId);
        requireUniqueName(request.name(), categoryId);
        CategoryMapper.applyUpdate(category, request);
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    /** PATCH /categories/{id}/status. Seul point d'entree pour activer/desactiver. */
    @Transactional
    public void updateStatus(UUID categoryId, boolean active) {
        Category category = requireCategory(categoryId);
        category.setActive(active);
        categoryRepository.save(category);
    }

    private void requireUniqueName(String name, UUID excludeId) {
        boolean exists = excludeId == null
                ? categoryRepository.existsByNameIgnoreCase(name.trim())
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), excludeId);
        if (exists) {
            throw new BusinessRuleException("Une categorie nommee '" + name.trim() + "' existe deja");
        }
    }

    private Category requireCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Categorie", categoryId));
    }


}