package io.github.jelenajjovanoski.releasetracker.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;

public interface ReleaseService {

    ReleaseResponse create(ReleaseRequest request);
    ReleaseResponse getById(UUID id);
    Page<ReleaseResponse> getAll(String statusLabel, String nameContains, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);
    ReleaseResponse update(UUID id, ReleaseRequest request);
    void delete(UUID id);
}
