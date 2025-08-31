package io.github.jelenajjovanoski.releasetracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class ReleaseControllerIT {

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
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final String API = "/api/v1/releases";
    private static final String RELEASE_NAME = "Release 1.0";
    private static final String RELEASE_DESC = "Initial drop";
    private static final String RELEASE_STATUS = "Created";

    @Test
    void createAndGetById_returnsCreatedRelease() throws Exception {

        MvcResult post = mockMvc.perform(post(API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultReleasePayload())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.lastUpdateAt").exists())
                .andReturn();

        String id = JsonPath.read(post.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get(API + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Release 1.0"))
                .andExpect(jsonPath("$.description").value("Initial drop"))
                .andExpect(jsonPath("$.status").value("Created"));
    }

    @Test
    void createReleaseWithDuplicateName_returnsConflict() throws Exception {
        performCreatedRelease("Unique 1", "x", "Created", LocalDate.now().plusDays(1));

        mockMvc.perform(post(API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(buildPayload("Unique 1", "x", "Created", LocalDate.now().plusDays(2)))))
                .andExpect(status().isConflict());
    }

    @Test
    void createWithInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(post(API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(buildPayload("Bad status", "x", "On MARS", LocalDate.now().plusDays(1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByNonExistingId_returnsNotFound() throws Exception {
        mockMvc.perform(get(API + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createAndGetAll_sortedByLastUpdatedWithPagination() throws Exception {

        performCreatedRelease("Rel A", "desc A", "Created", LocalDate.now().plusDays(2));
        Thread.sleep(5);
        performCreatedRelease("Rel B", "desc B", "Created", LocalDate.now().plusDays(3));

        mockMvc.perform(get(API).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[0].name", anyOf(is("Rel B"), is("Re"))));

        mockMvc.perform(get(API).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.number", is(1)))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(2)));
    }

    @Test
    void createAndGetAll_withFilters_nameAndReleaseDateRange() throws Exception {

        performCreatedRelease("Rel Filter X", "f", "Created", LocalDate.of(2025, 9, 10));
        performCreatedRelease("Other", "f", "QA done on STAGING", LocalDate.of(2025, 10, 1));

        mockMvc.perform(get(API)
                        .param("nameContains", "rel fil")
                        .param("releaseDateFrom", "2025-09-01")
                        .param("releaseDateTo", "2025-09-30")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].name").value("Rel Filter X"))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void updateWithValidRequest_returnsOkAndUpdatesFields() throws Exception {
        UUID id = postRelease("Rel C", "Old", "Created", LocalDate.now().plusDays(1));

        mockMvc.perform(put(API + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(buildPayload("Rel C", "New Desc", "On DEV", LocalDate.now().plusDays(2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("Rel C")))
                .andExpect(jsonPath("$.description", is("New Desc")))
                .andExpect(jsonPath("$.status", is("On DEV")));
    }

    @Test
    void updateNonExisting_returnsNotFound() throws Exception {
        UUID missing = UUID.randomUUID();
        var updateRequest = new ReleaseRequest("Rel X", "Desc", "Created", LocalDate.now().plusDays(1));

        mockMvc.perform(put(API + "/{id}", missing)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWithDuplicateName_returnsConflict() throws Exception {
        UUID id1 = postRelease("Rel D", "First", "Created", LocalDate.now().plusDays(1));
        UUID id2 = postRelease("Rel E", "Second", "Created", LocalDate.now().plusDays(2));

        ReleaseRequest updateRequest  = new ReleaseRequest("Rel D", "Second UPDATED", "On DEV", LocalDate.now().plusDays(3));

        mockMvc.perform(put(API + "/{id}", id2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateWithInvalidStatus_returnsBadRequest() throws Exception {
        UUID id = postRelease("Rel F", "Desc", "Created", LocalDate.now().plusDays(1));

        var req = new ReleaseRequest("Rel G", "Desc", "INVALID_STATUS", LocalDate.now().plusDays(1));

        mockMvc.perform(put(API + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }


    private Map<String, String> buildPayload(String name, String description, String status, LocalDate releaseDate) {
        return Map.of(
                "name", name,
                "description", description,
                "status", status,
                "releaseDate", releaseDate.toString()
        );
    }

    private Map<String, String> defaultReleasePayload() {
        return Map.of(
                "name", RELEASE_NAME,
                "description", RELEASE_DESC,
                "status", RELEASE_STATUS,
                "releaseDate", LocalDate.now().plusDays(1).toString()
        );
    }

    private UUID postRelease(String name, String desc, String status, LocalDate date) throws Exception {
        String location = performCreatedRelease(name, desc, status, date)
                .getResponse()
                .getHeader("Location");
        String idStr = location.substring(location.lastIndexOf('/') + 1);
        return UUID.fromString(idStr);
    }

    private MvcResult performCreatedRelease(String name, String desc, String status, LocalDate date) throws Exception {
        return mockMvc.perform(post(API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(buildPayload(name, desc, status, date))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }
}
