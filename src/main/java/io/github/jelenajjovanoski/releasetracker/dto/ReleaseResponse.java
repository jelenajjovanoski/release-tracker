package io.github.jelenajjovanoski.releasetracker.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReleaseResponse(
        UUID id,
        String name,
        String description,
        String status,
        LocalDate releaseDate,
        OffsetDateTime createdAt,
        OffsetDateTime lastUpdateAt
) {}
