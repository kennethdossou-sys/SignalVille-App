package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.api.dto.response.PhotoResponse;
import africa.epf.signalville_backend.application.mapper.InterventionProofMapper;
import africa.epf.signalville_backend.application.service.InterventionService;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.InterventionProof;
import africa.epf.signalville_backend.infrastructure.security.AppUserPrincipal;
import africa.epf.signalville_backend.infrastructure.storage.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Expose les preuves de resolution d'une intervention (InterventionProof).
 * Volontairement distinct de PhotoController : meme mecanique technique de
 * streaming binaire, mais deux ressources metier differentes (photo de depot
 * du citoyen vs preuve de resolution de l'agent) - separation actee avec le
 * Product Owner, non negociable.
 *
 * Ce controller ne porte pour l'instant que les routes de LECTURE des preuves.
 * Les 7 routes d'ecriture du Module 2 (assign, reassign, start, resolve,
 * close, reopen, reject) seront ajoutees ici par la suite, sans toucher a
 * ce qui existe deja.
 */
@RestController
@RequestMapping("/interventions")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionService interventionService;
    private final PhotoStorageService photoStorage;

    /** GET /interventions/{interventionId}/proofs : liste des metadonnees, pas le binaire. */
    @GetMapping("/{interventionId}/proofs")
    public ResponseEntity<List<PhotoResponse>> listProofs(@AuthenticationPrincipal AppUserPrincipal principal,
                                                           @PathVariable UUID interventionId) {
        List<InterventionProof> proofs = interventionService.listVisibleProofs(principal, interventionId);
        return ResponseEntity.ok(proofs.stream().map(InterventionProofMapper::toResponse).toList());
    }

    /**
     * GET /interventions/{interventionId}/proofs/{proofId} : binaire de la preuve.
     * Meme mecanique que PhotoController.download (Resource, Content-Type stocke
     * en base, cache prive 1h, Content-Disposition inline). Le front devra
     * recuperer ce binaire en Blob authentifie, exactement comme pour
     * getPhotoBlob() sur les photos de signalement.
     */
    @GetMapping("/{interventionId}/proofs/{proofId}")
    public ResponseEntity<Resource> downloadProof(@AuthenticationPrincipal AppUserPrincipal principal,
                                                   @PathVariable UUID interventionId,
                                                   @PathVariable UUID proofId) {
        InterventionProof proof = interventionService.requireVisibleProof(principal, interventionId, proofId);
        Path file = photoStorage.resolve(proof.getStoragePath());

        if (!Files.isReadable(file)) {
            throw ResourceNotFoundException.of("Fichier de la preuve", proofId);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(proof.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + proofId + "\"")
                .body(new FileSystemResource(file));
    }
}