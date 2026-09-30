package africa.epf.signalville_backend.infrastructure.persistence;

import africa.epf.signalville_backend.domain.model.AccountStatus;
import africa.epf.signalville_backend.domain.model.Role;
import africa.epf.signalville_backend.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("UserRepository - Tests d'intégration")
class UserRepositoryIT {


    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("save() persiste un utilisateur et lui assigne un UUID")
    void save_should_persist_user_and_assign_uuid() {
        // GIVEN
        User user = User.builder()
                .firstName("Kenneth")
                .lastName("Dossou")
                .email("ken@signalville.test")
                .phone("+221770000000")
                .passwordHash("hash-fictif")
                .role(Role.CITOYEN)
                .status(AccountStatus.ACTIF)
                .build();

        // WHEN
        User saved = userRepository.save(user);

        // THEN
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmailIgnoreCase retrouve un utilisateur quelle que soit la casse")
    void findByEmailIgnoreCase_should_be_case_insensitive() {
        // GIVEN
        User user = User.builder()
                .firstName("Kenneth")
                .lastName("Dossou")
                .email("ken@signalville.test")
                .phone("+221770000000")
                .passwordHash("hash-fictif")
                .role(Role.CITOYEN)
                .status(AccountStatus.ACTIF)
                .build();
        userRepository.save(user);

        // WHEN
        Optional<User> found = userRepository.findByEmailIgnoreCase("KEN@SIGNALVILLE.TEST");

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("ken@signalville.test");
    }

    @Test
    @DisplayName("existsByEmailIgnoreCase renvoie true si l'email existe, ignorant la casse")
    void existsByEmailIgnoreCase_should_detect_existing_email() {
        // GIVEN
        User user = User.builder()
                .firstName("Kenneth")
                .lastName("Dossou")
                .email("ken@signalville.test")
                .phone("+221770000000")
                .passwordHash("hash-fictif")
                .role(Role.CITOYEN)
                .status(AccountStatus.ACTIF)
                .build();
        userRepository.save(user);

        // WHEN & THEN
        assertThat(userRepository.existsByEmailIgnoreCase("KEN@SIGNALVILLE.TEST")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("inconnu@signalville.test")).isFalse();
    }
}