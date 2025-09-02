package io.github.jelenajjovanoski.releasetracker.dto;

import java.util.Map;
import java.time.OffsetDateTime;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {}
