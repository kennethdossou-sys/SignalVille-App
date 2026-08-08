package africa.epf.signalville_backend.application.service;

import africa.epf.signalville_backend.api.dto.response.CategoryResponse;
import africa.epf.signalville_backend.application.mapper.CategoryMapper;
import africa.epf.signalville_backend.infrastructure.persistence.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
