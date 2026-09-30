package africa.epf.signalville_backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "signalville.storage")
public record StorageProperties(
        String uploadDir,
        long maxPhotoSizeBytes,
        List<String> allowedContentTypes
) {
}
