package io.github.jelenajjovanoski.releasetracker.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ReleaseRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        @Size(max = 5000, message = "Description too long")
        String description,

        @NotNull(message = "Status is required")
        String status,

        @FutureOrPresent(message = "Release date must be today or in the future")
        LocalDate releaseDate
) {}
