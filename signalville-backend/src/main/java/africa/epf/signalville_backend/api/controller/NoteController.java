package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.request.CreateNoteRequest;
import africa.epf.signalville_backend.api.dto.response.NoteResponse;
import africa.epf.signalville_backend.application.mapper.NoteMapper;
import africa.epf.signalville_backend.application.service.NoteService;
import africa.epf.signalville_backend.domain.model.ReportNote;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Notes attachees a un signalement. Lecture filtree par role dans le service
 * (INTERNE invisible au citoyen). Ecriture : voir @PreAuthorize ci-dessous -
 * DECISION EN ATTENTE DE CONFIRMATION (cf. echange), valeur par defaut
 * appliquee : AGENT et SUPERVISEUR uniquement.
 */
@RestController
@RequestMapping("/reports/{reportId}/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ResponseEntity<List<NoteResponse>> list(@AuthenticationPrincipal AppUserPrincipal principal,
                                                    @PathVariable UUID reportId) {
        List<ReportNote> notes = noteService.list(principal, reportId);
        return ResponseEntity.ok(notes.stream().map(NoteMapper::toResponse).toList());
    }

    /**
     * Reserve a AGENT et SUPERVISEUR. Le CITOYEN ne cree jamais de note (interne
     * ou publique) : si un canal d'echange citoyen-service est ajoute plus tard,
     * il devra passer par un mecanisme distinct, pas par ce endpoint.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISEUR')")
    public ResponseEntity<NoteResponse> add(@AuthenticationPrincipal AppUserPrincipal principal,
                                            @PathVariable UUID reportId,
                                            @Valid @RequestBody CreateNoteRequest request) {
        ReportNote note = noteService.add(principal, reportId, request.content(), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteMapper.toResponse(note));
    }
}