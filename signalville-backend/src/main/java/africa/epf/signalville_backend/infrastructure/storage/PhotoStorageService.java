package africa.epf.signalville_backend.infrastructure.storage;

import africa.epf.signalville_backend.domain.exception.BusinessRuleException;
import africa.epf.signalville_backend.domain.exception.StorageException;
import africa.epf.signalville_backend.infrastructure.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Stockage des photos sur le systeme de fichiers, sous
 * {uploadDir}/{reportId}/{uuid}.{ext}. Seul le chemin relatif est persiste en base.
 */
@Service
@Slf4j
public class PhotoStorageService {

    private final StorageProperties properties;
    private final Path rootDirectory;

    public PhotoStorageService(StorageProperties properties) {
        this.properties = properties;
        this.rootDirectory = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException ex) {
            throw new StorageException("Impossible de creer le repertoire de stockage " + rootDirectory, ex);
        }
    }

    /** Rejette taille et type non conformes avant toute ecriture disque. */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Une photo transmise est vide");
        }
        if (file.getSize() > properties.maxPhotoSizeBytes()) {
            throw new BusinessRuleException(
                    "Photo trop volumineuse (%s) : 5 Mo maximum".formatted(file.getOriginalFilename()));
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!properties.allowedContentTypes().contains(contentType)) {
            throw new BusinessRuleException(
                    "Format non supporte (%s) : JPEG ou PNG uniquement".formatted(contentType));
        }
    }

    /** Ecrit le fichier et retourne le chemin relatif a la racine de stockage. */
    public String store(UUID reportId, MultipartFile file) {
        validate(file);
        Path reportDirectory = rootDirectory.resolve(reportId.toString());
        String storedName = UUID.randomUUID() + extensionFor(file);
        Path target = reportDirectory.resolve(storedName);

        try {
            Files.createDirectories(reportDirectory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new StorageException("Echec d'ecriture de la photo " + storedName, ex);
        }

        return rootDirectory.relativize(target).toString().replace('\\', '/');
    }

    public Path resolve(String relativePath) {
        Path resolved = rootDirectory.resolve(relativePath).normalize();
        // Garde-fou contre une traversee de repertoire via un chemin corrompu en base.
        if (!resolved.startsWith(rootDirectory)) {
            throw new StorageException("Chemin de fichier hors du repertoire de stockage : " + relativePath, null);
        }
        return resolved;
    }

    public void deleteQuietly(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException | StorageException ex) {
            log.warn("Suppression impossible pour {} : {}", relativePath, ex.getMessage());
        }
    }

    private String extensionFor(MultipartFile file) {
        String original = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (original != null && !original.isBlank()) {
            return "." + original.toLowerCase(Locale.ROOT);
        }
        return "image/png".equalsIgnoreCase(file.getContentType()) ? ".png" : ".jpg";
    }
}
