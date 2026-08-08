package africa.epf.signalville_backend.api.controller;

import africa.epf.signalville_backend.application.service.ReportService;
import africa.epf.signalville_backend.domain.exception.ResourceNotFoundException;
import africa.epf.signalville_backend.domain.model.ReportPhoto;
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
import java.util.UUID;

/**
 * Sert les binaires de photos. Endpoint authentifie : le controle de propriete
 * est delegue a ReportService, comme pour la lecture du signalement lui-meme.
 *
 * Consequence cote client : une balise <img src> ne peut pas porter le header
 * Authorization. Le front recupere donc l'image via HttpClient (responseType blob)
 * puis construit une object URL. C'est un choix assume pour ne pas exposer
 * les fichiers en acces anonyme.
 */
@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final ReportService reportService;
    private final PhotoStorageService photoStorage;

    @GetMapping("/{photoId}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AppUserPrincipal principal,
                                             @PathVariable UUID photoId) {
        ReportPhoto photo = reportService.requireVisiblePhoto(principal, photoId);
        Path file = photoStorage.resolve(photo.getStoragePath());

        if (!Files.isReadable(file)) {
            throw ResourceNotFoundException.of("Fichier de la photo", photoId);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + photoId + "\"")
                .body(new FileSystemResource(file));
    }
}
