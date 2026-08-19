package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.request.AssignReportRequest;
import africa.epf.signalville_backend.api.dto.request.CloseReportRequest;
import africa.epf.signalville_backend.api.dto.request.ReassignReportRequest;
import africa.epf.signalville_backend.api.dto.request.RejectReportRequest;
import africa.epf.signalville_backend.api.dto.request.ReopenReportRequest;
import africa.epf.signalville_backend.api.dto.request.ResolveReportFormRequest;
import africa.epf.signalville_backend.api.dto.response.InterventionResponse;
import africa.epf.signalville_backend.application.mapper.InterventionMapper;
import africa.epf.signalville_backend.application.mapper.ReportMapper;
import africa.epf.signalville_backend.application.service.InterventionService;
import africa.epf.signalville_backend.domain.model.Intervention;
import africa.epf.signalville_backend.domain.model.Report;
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
    private final InterventionService interventionService;

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
    /** POST /reports/{id}/reject : reserve au superviseur, depuis NOUVEAU uniquement. */
    @PostMapping("/{reportId}/reject")
    @PreAuthorize("hasRole('SUPERVISEUR')")
    public ResponseEntity<ReportResponse> reject(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @PathVariable UUID reportId,
                                                 @Valid @RequestBody RejectReportRequest request) {
        return ResponseEntity.ok(reportService.reject(principal, reportId, request.reason()));
    }

    /** POST /reports/{id}/assign : cree l'intervention, reserve au superviseur. */
    @PostMapping("/{reportId}/assign")
    @PreAuthorize("hasRole('SUPERVISEUR')")
    public ResponseEntity<InterventionResponse> assign(@AuthenticationPrincipal AppUserPrincipal principal,
                                                        @PathVariable UUID reportId,
                                                        @Valid @RequestBody AssignReportRequest request) {
        Intervention intervention = interventionService.assign(
                principal, reportId, request.agentId(), request.instruction());
        return ResponseEntity.status(HttpStatus.CREATED).body(InterventionMapper.toResponse(intervention));
    }

    /** POST /reports/{id}/reassign : cloture l'intervention active, en cree une nouvelle. */
    @PostMapping("/{reportId}/reassign")
    @PreAuthorize("hasRole('SUPERVISEUR')")
    public ResponseEntity<InterventionResponse> reassign(@AuthenticationPrincipal AppUserPrincipal principal,
                                                          @PathVariable UUID reportId,
                                                          @Valid @RequestBody ReassignReportRequest request) {
        Intervention intervention = interventionService.reassign(
                principal, reportId, request.newAgentId(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(InterventionMapper.toResponse(intervention));
    }

    /** POST /reports/{id}/start : reserve a l'agent affecte a l'intervention active. */
    @PostMapping("/{reportId}/start")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<InterventionResponse> start(@AuthenticationPrincipal AppUserPrincipal principal,
                                                       @PathVariable UUID reportId) {
        Intervention intervention = interventionService.start(principal, reportId);
        return ResponseEntity.ok(InterventionMapper.toResponse(intervention));
    }

    /**
     * POST /reports/{id}/resolve (multipart/form-data) : commentaire + 1 a 3 preuves,
     * reserve a l'agent affecte. Meme mecanique de binding que create()/update().
     */
    @PostMapping(path = "/{reportId}/resolve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<InterventionResponse> resolve(@AuthenticationPrincipal AppUserPrincipal principal,
                                                         @PathVariable UUID reportId,
                                                         @Valid @ModelAttribute ResolveReportFormRequest form,
                                                         @RequestPart("proofs") List<MultipartFile> proofs) {
        Intervention intervention = interventionService.resolve(
                principal, reportId, form.resolutionComment(), proofs);
        return ResponseEntity.ok(InterventionMapper.toResponse(intervention));
    }

    /** POST /reports/{id}/close : reserve au superviseur, depuis RESOLU uniquement. */
    @PostMapping("/{reportId}/close")
    @PreAuthorize("hasRole('SUPERVISEUR')")
    public ResponseEntity<ReportResponse> close(@AuthenticationPrincipal AppUserPrincipal principal,
                                                @PathVariable UUID reportId,
                                                @RequestBody(required = false) CloseReportRequest request) {
        String comment = request == null ? null : request.publicComment();
        Report report = interventionService.close(principal, reportId, comment);
        return ResponseEntity.ok(ReportMapper.toResponse(report));
    }

    /** POST /reports/{id}/reopen : reserve au superviseur, depuis CLOTURE uniquement, motif obligatoire. */
    @PostMapping("/{reportId}/reopen")
    @PreAuthorize("hasRole('SUPERVISEUR')")
    public ResponseEntity<ReportResponse> reopen(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @PathVariable UUID reportId,
                                                 @Valid @RequestBody ReopenReportRequest request) {
        Report report = interventionService.reopen(principal, reportId, request.reason());
        return ResponseEntity.ok(ReportMapper.toResponse(report));
    }
}
