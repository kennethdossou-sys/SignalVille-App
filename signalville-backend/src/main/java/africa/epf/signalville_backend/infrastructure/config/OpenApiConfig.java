package africa.epf.signalville_backend.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declare le schema d'authentification Bearer pour springdoc, afin que
 * Swagger UI affiche le bouton "Authorize" et joigne automatiquement le
 * token JWT aux requetes "Try it out". Sans ce bean, springdoc genere un
 * contrat sans securitySchemes, meme si SecurityConfig protege reellement
 * les routes.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI signalVilleOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API SignalVille")
                        .version("1.1.0")
                        .description("API REST de la plateforme SignalVille."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}