package io.github.jelenajjovanoski.releasetracker.service;

import io.github.jelenajjovanoski.releasetracker.exception.NameAlreadyExistsException;
import io.github.jelenajjovanoski.releasetracker.exception.ResourceNotFoundException;
import io.github.jelenajjovanoski.releasetracker.mapper.ReleaseMapper;
import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import io.github.jelenajjovanoski.releasetracker.repository.ReleaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static io.github.jelenajjovanoski.releasetracker.repository.ReleaseSpecifications.*;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ReleaseServiceImpl implements ReleaseService {

    private static final Logger log = LoggerFactory.getLogger(ReleaseServiceImpl.class);

    private final ReleaseRepository repo;
    private final ReleaseMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public ReleaseServiceImpl(ReleaseRepository repo, ReleaseMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public ReleaseResponse create(ReleaseRequest r) {
        log.debug("Creating new release with name='{}'", r.name());
        if (repo.existsByName(r.name())) {
            log.warn("Attempt to create release with duplicate name {}", r.name());
            throw new NameAlreadyExistsException(r.name());
        }
        Release entity = mapper.toEntity(r);
        Release saved = repo.save(entity);

        log.info("Release created with id={} and status={}", saved.getId(), saved.getStatus());
        return mapper.toResponse(saved);
    }

    @Override
    public ReleaseResponse getById(UUID id) {
        Release release = repo.findById(id)
                .orElseThrow(() ->  new ResourceNotFoundException("Release with id " + id + " not found"));
        log.debug("Getting release with id={}", release);
        return mapper.toResponse(release);
    }

    @Override
    public Page<ReleaseResponse> getAll(String statusLabel, String nameContains, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        ReleaseStatus status = null;
        if (statusLabel != null && !statusLabel.isBlank()) {
            status = ReleaseStatus.fromLabel(statusLabel);
        }

        Specification<Release> spec = Specification.allOf(
                hasStatus(status),
                nameContains(nameContains),
                releaseDateFrom(dateFrom),
                releaseDateTo(dateTo)
        );

        Pageable pageableWithDefaultSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted()
                        ? pageable.getSort()
                        : Sort.by(Sort.Direction.DESC, "lastUpdateAt"));

        Page<Release> page = repo.findAll(spec, pageableWithDefaultSort);
        return page.map(mapper::toResponse);
    }
}
