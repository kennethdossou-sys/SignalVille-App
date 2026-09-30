package africa.epf.signalville_backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "signalville.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
