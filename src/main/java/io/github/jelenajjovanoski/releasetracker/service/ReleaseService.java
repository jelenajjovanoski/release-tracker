package io.github.jelenajjovanoski.releasetracker.service;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface ReleaseService {

    ReleaseResponse create(ReleaseRequest request);
    ReleaseResponse getById(UUID id);
    Page<ReleaseResponse> getAll(String statusLabel, String nameContains, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);
    ReleaseResponse update(UUID id, ReleaseRequest request);
}
