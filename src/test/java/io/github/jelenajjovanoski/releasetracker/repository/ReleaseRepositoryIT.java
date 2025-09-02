package io.github.jelenajjovanoski.releasetracker.repository;

import java.time.LocalDate;
import java.util.List;

import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.github.jelenajjovanoski.releasetracker.repository.ReleaseSpecifications.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
public class ReleaseRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.flyway.enabled", () -> "false");
        r.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    ReleaseRepository repo;

    @BeforeEach
    void seed() {
        repo.deleteAll();

        Release r1 = new Release();
        r1.setName("Test A");
        r1.setDescription("A");
        r1.setStatus(ReleaseStatus.CREATED);
        r1.setReleaseDate(LocalDate.of(2025, 1, 10));
        repo.save(r1);

        Release r2 = new Release();
        r2.setName("Rel B");
        r2.setDescription("B");
        r2.setStatus(ReleaseStatus.ON_DEV);
        r2.setReleaseDate(LocalDate.of(2025, 2, 5));
        repo.save(r2);

        Release r3 = new Release();
        r3.setName("Release 1.0");
        r3.setDescription("C");
        r3.setStatus(ReleaseStatus.DONE);
        r3.setReleaseDate(LocalDate.of(2025, 3, 1));
        repo.save(r3);
    }

    @Test
    void findAll_whenNameContains_shouldReturnMatching() {
        var spec = nameContains("rel");
        List<Release> result = repo.findAll(spec);
        assertThat(result).extracting(Release::getName)
                .containsExactlyInAnyOrder("Rel B", "Release 1.0");
    }

    @Test
    void findAll_whenDateInRange_shouldReturnSubset() {
        var spec = releaseDateFrom(LocalDate.of(2025, 2, 1))
                .and(releaseDateTo(LocalDate.of(2025, 3, 1)));
        List<Release> result = repo.findAll(spec);
        assertThat(result).extracting(Release::getName)
                .containsExactlyInAnyOrder("Rel B", "Release 1.0");
    }

    @Test
    void findAll_whenStatusProvided_shouldReturnOnlyMatching() {
        var spec = hasStatus(ReleaseStatus.ON_DEV);
        List<Release> result = repo.findAll(spec);
        assertThat(result).singleElement()
                .extracting(Release::getName).isEqualTo("Rel B");
    }

    @Test
    void findByStatus_whenValidEnum_shouldReturnMatchingReleases() {
        List<Release> result = repo.findByStatus(ReleaseStatus.DONE);
        assertThat(result).singleElement()
                .extracting(Release::getName).isEqualTo("Release 1.0");
    }

    @Test
    void existsByName_whenNameExists_shouldReturnTrue() {
        boolean exists = repo.existsByName("Test A");
        assertThat(exists).isTrue();
    }
}
