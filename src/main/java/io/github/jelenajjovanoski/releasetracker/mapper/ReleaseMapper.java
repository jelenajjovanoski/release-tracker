package io.github.jelenajjovanoski.releasetracker.mapper;

import org.springframework.stereotype.Component;

import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;

@Component
public class ReleaseMapper {

    public Release toEntity(ReleaseRequest r) {
        Release e = new Release();
        e.setName(r.name());
        e.setDescription(r.description());
        e.setStatus(ReleaseStatus.fromLabel(r.status()));
        e.setReleaseDate(r.releaseDate());
        return e;
    }

    public ReleaseResponse toResponse(Release r) {
        return new ReleaseResponse(
                r.getId(),
                r.getName(),
                r.getDescription(),
                r.getStatus().getLabel(),
                r.getReleaseDate(),
                r.getCreatedAt(),
                r.getLastUpdateAt()
        );
    }
}
