package io.github.jelenajjovanoski.releasetracker.service;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;

import java.util.UUID;

public interface ReleaseService {

    ReleaseResponse create(ReleaseRequest request);
    ReleaseResponse getById(UUID id);
}
