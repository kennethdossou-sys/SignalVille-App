package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.request.CancelReportRequest;
import africa.epf.signalville_backend.api.dto.request.ReportFormRequest;
import africa.epf.signalville_backend.api.dto.response.PageResponse;
import africa.epf.signalville_backend.api.dto.response.ReportDetailResponse;
import africa.epf.signalville_backend.api.dto.response.ReportResponse;
import africa.epf.signalville_backend.application.service.ReportService;
import africa.epf.signalville_backend.domain.model.Priority;
import africa.epf.signalville_backend.domain.model.ReportStatus;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Validated
public class ReportController {

    private final ReportService reportService;

    /**
     * POST /reports (multipart/form-data). Reserve aux citoyens : un agent ou un
     * superviseur ne depose pas de signalement en son nom propre.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITOYEN')")
    public ResponseEntity<ReportResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @Valid @ModelAttribute ReportFormRequest form,
                                                 @RequestPart("photos") List<MultipartFile> photos) {
        ReportResponse created = reportService.create(principal, form, photos);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /reports. Le perimetre depend du role : le citoyen ne recoit que ses
     * propres signalements (filtre applique dans le service, pas cote client).
     */
    @GetMapping
    public ResponseEntity<PageResponse<ReportResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(reportService.list(principal, status, categoryId, priority, search, pageable));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDetailResponse> detail(@AuthenticationPrincipal AppUserPrincipal principal,
                                                       @PathVariable UUID reportId) {
        return ResponseEntity.ok(reportService.getDetail(principal, reportId));
    }

    /** PUT /reports/{id} : 409 si le signalement n'est plus au statut NOUVEAU. */
    @PutMapping(path = "/{reportId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITOYEN')")
    public ResponseEntity<ReportResponse> update(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @PathVariable UUID reportId,
                                                 @Valid @ModelAttribute ReportFormRequest form,
                                                 @RequestPart("photos") List<MultipartFile> photos) {
        return ResponseEntity.ok(reportService.update(principal, reportId, form, photos));
    }

    /** POST /reports/{id}/cancel : le contrat renvoie 200 avec le signalement mis a jour. */
    @PostMapping("/{reportId}/cancel")
    @PreAuthorize("hasRole('CITOYEN')")
    public ResponseEntity<ReportResponse> cancel(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @PathVariable UUID reportId,
                                                 @Valid @RequestBody CancelReportRequest request) {
        return ResponseEntity.ok(reportService.cancel(principal, reportId, request.reason()));
    }
}
