package io.github.jelenajjovanoski.releasetracker.service;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


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
        r.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final String API_RELEASES = "/api/releases";
    private static final String RELEASE_NAME = "Release 1.0";
    private static final String RELEASE_DESC = "Initial drop";
    private static final String RELEASE_STATUS = "Created";

    @Test
    void createAndGetById() throws Exception {

        MvcResult post = mockMvc.perform(post(API_RELEASES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReleaseRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.lastUpdateAt").exists())
                .andReturn();

        String id = JsonPath.read(post.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get(API_RELEASES + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(RELEASE_NAME))
                .andExpect(jsonPath("$.description").value(RELEASE_DESC))
                .andExpect(jsonPath("$.status").value(RELEASE_STATUS));
    }

    private Map<String, String> newReleaseRequest() {
        return Map.of(
                "name", RELEASE_NAME,
                "description", RELEASE_DESC,
                "status", RELEASE_STATUS,
                "releaseDate", LocalDate.now().plusDays(1).toString()
        );
    }
}
