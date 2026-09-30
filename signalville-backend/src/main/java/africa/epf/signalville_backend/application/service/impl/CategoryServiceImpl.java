package africa.epf.signalville_backend.application.service.impl;

import africa.epf.signalville_backend.api.dto.request.CategoryRequest;
import africa.epf.signalville_backend.api.dto.response.CategoryResponse;
import africa.epf.signalville_backend.application.mapper.CategoryMapper;
import africa.epf.signalville_backend.application.service.CategoryService;
import africa.epf.signalville_backend.domain.exception.BusinessRuleException;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.Category;
import africa.epf.signalville_backend.infrastructure.persistence.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation de CategoryService. Code inchange par rapport a l'ancienne
 * classe CategoryService — seul l'emplacement et le nom changent (Seance 5,
 * retour du professeur : separation interface/implementation par service).
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .map(CategoryMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Categorie", categoryId));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        requireUniqueName(request.name(), null);
        Category saved = categoryRepository.save(CategoryMapper.toEntity(request));
        return CategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID categoryId, CategoryRequest request) {
        Category category = requireCategory(categoryId);
        requireUniqueName(request.name(), categoryId);
        CategoryMapper.applyUpdate(category, request);
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Override
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