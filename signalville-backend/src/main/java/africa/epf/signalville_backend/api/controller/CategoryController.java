package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.request.CategoryRequest;
import africa.epf.signalville_backend.api.dto.response.CategoryResponse;
import africa.epf.signalville_backend.application.service.CategoryService;
import africa.epf.signalville_backend.api.dto.request.UpdateCategoryStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** Public (security: [] dans le contrat) : le formulaire de signalement en a besoin avant login. */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listActive() {
        return ResponseEntity.ok(categoryService.listActive());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<CategoryResponse>> listAll() {
        return ResponseEntity.ok(categoryService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID categoryId,
                                                    @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(categoryId, request));
    }

    @PatchMapping("/{categoryId}/status")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID categoryId,
                                          @Valid @RequestBody UpdateCategoryStatusRequest request) {
    categoryService.updateStatus(categoryId, request.active());
    return ResponseEntity.noContent().build();
    }

    @GetMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<CategoryResponse> getById(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(categoryService.getById(categoryId));
    }
}