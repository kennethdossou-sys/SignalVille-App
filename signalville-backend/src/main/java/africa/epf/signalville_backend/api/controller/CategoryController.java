package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.response.CategoryResponse;
import africa.epf.signalville_backend.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
