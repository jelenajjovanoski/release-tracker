package io.github.jelenajjovanoski.releasetracker.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

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

    @PostMapping
    public ResponseEntity<ReleaseResponse> create(@RequestBody @Valid ReleaseRequest r) {
        ReleaseResponse response = releaseService.create(r);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReleaseResponse> get(@PathVariable UUID id) {
        ReleaseResponse response = releaseService.getById(id);
        return ResponseEntity.ok(response);
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<ReleaseResponse> update(@PathVariable UUID id, @RequestBody @Valid ReleaseRequest request) {
        ReleaseResponse updated = releaseService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        releaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
