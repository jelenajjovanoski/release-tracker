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

    @Test
    void testCreateAndGetById_returnsCreatedRelease() throws Exception {

        String id = postRelease("Release 1.0", "Initial drop", "Created", LocalDate.now().plusDays(1));

        mockMvc.perform(get(API + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Release 1.0"))
                .andExpect(jsonPath("$.description").value("Initial drop"))
                .andExpect(jsonPath("$.status").value("Created"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.lastUpdateAt").exists());
    }

    @Test
    void testCreateWithDuplicateName_returnsConflict() throws Exception {
        postRelease("Unique 1", "x", "Created", LocalDate.now().plusDays(1));

        mockMvc.perform(post(API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Unique 1", "x", "Created", LocalDate.now().plusDays(2)))))
                .andExpect(status().isConflict());
    }

    @Test
    void testCreateWithInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(post(API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Bad status", "x", "On MARS", LocalDate.now().plusDays(1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetByNonExistingId_returnsNotFound() throws Exception {
        mockMvc.perform(get(API + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAll_sortedByLastUpdatedWithPagination() throws Exception {
        postRelease("Rel A", "desc A", "Created", LocalDate.now().plusDays(2));
        Thread.sleep(5);
        postRelease("Rel B", "desc B", "Created", LocalDate.now().plusDays(3));

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
    void testGetAll_withFilters_nameAndReleaseDateRange() throws Exception {
        postRelease("Rel Filter X", "f", "Created", LocalDate.of(2025, 9, 10));
        postRelease("Other", "f", "QA done on STAGING", LocalDate.of(2025, 10, 1));

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
    void testUpdateWithValidRequest_returnsOkAndUpdatesFields() throws Exception {
        String id = postRelease("Rel C", "Old", "Created", LocalDate.now().plusDays(1));

        mockMvc.perform(put(API + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Rel C", "New Desc", "On DEV", LocalDate.now().plusDays(2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.name", is("Rel C")))
                .andExpect(jsonPath("$.description", is("New Desc")))
                .andExpect(jsonPath("$.status", is("On DEV")));
    }

    @Test
    void testUpdateNonExisting_returnsNotFound() throws Exception {
        mockMvc.perform(put(API + "/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Rel X", "Desc", "Created", LocalDate.now().plusDays(1)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateWithDuplicateName_returnsConflict() throws Exception {
        String id1 = postRelease("Rel D", "First", "Created", LocalDate.now().plusDays(1));
        String id2 = postRelease("Rel E", "Second", "Created", LocalDate.now().plusDays(2));

        mockMvc.perform(put(API + "/{id}", id2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload(
                                "Rel D", "Second UPDATED", "On DEV", LocalDate.now().plusDays(3)))))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateWithInvalidStatus_returnsBadRequest() throws Exception {
        String id = postRelease("Rel F", "Desc", "Created", LocalDate.now().plusDays(1));

        mockMvc.perform(put(API + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Rel G", "Desc", "INVALID_STATUS", LocalDate.now().plusDays(1)))))
                .andExpect(status().isBadRequest());
    }

    private String postRelease(String name, String desc, String status, LocalDate date) throws Exception {
        MvcResult res = mockMvc.perform(post(API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload(name, desc, status, date))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.lastUpdateAt").exists())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        return JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    private Map<String, String> payload(String name, String description, String status, LocalDate releaseDate) {
        return Map.of(
                "name", name,
                "description", description,
                "status", status,
                "releaseDate", releaseDate.toString()
        );
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }
}
