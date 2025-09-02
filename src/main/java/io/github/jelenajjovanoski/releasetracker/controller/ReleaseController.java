package io.github.jelenajjovanoski.releasetracker.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;
import io.github.jelenajjovanoski.releasetracker.service.ReleaseService;

@RestController
@RequestMapping("/api/v1/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @Operation(summary = "Create a new release", description = "Creates a new release entity and returns it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Release created",
                    content = @Content(schema = @Schema(implementation = ReleaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "409", description = "Release with the same name already exists", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ReleaseResponse> create(@RequestBody @Valid ReleaseRequest request) {
        ReleaseResponse response = releaseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get release by ID", description = "Fetch a single release by its UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Release found",
                    content = @Content(schema = @Schema(implementation = ReleaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Release not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReleaseResponse> get(@PathVariable UUID id) {
        ReleaseResponse response = releaseService.getById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all releases with filters", description = "Returns paginated list of releases filtered by status, name or release date range.")
    @ApiResponse(responseCode = "200", description = "List of releases")
    @GetMapping
    public ResponseEntity<Page<ReleaseResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nameContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReleaseResponse> result = releaseService.getAll(status, nameContains, releaseDateFrom, releaseDateTo, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Update release", description = "Updates an existing release by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Release updated",
                    content = @Content(schema = @Schema(implementation = ReleaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Release not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Release with the same name already exists", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<ReleaseResponse> update(@PathVariable UUID id, @RequestBody @Valid ReleaseRequest request) {
        ReleaseResponse updated = releaseService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete release", description = "Deletes an existing release by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Release deleted"),
            @ApiResponse(responseCode = "404", description = "Release not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        releaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
