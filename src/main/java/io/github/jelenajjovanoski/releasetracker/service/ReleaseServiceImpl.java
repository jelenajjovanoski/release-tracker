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
import org.springframework.transaction.annotation.Transactional;

import static io.github.jelenajjovanoski.releasetracker.repository.ReleaseSpecifications.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
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

    @Transactional
    @Override
    public ReleaseResponse create(ReleaseRequest r) {
        long t0 = System.nanoTime();
        log.debug("Create called release name='{}'", r.name());
        if (repo.existsByName(r.name())) {
            log.warn("Create blocked: release name already exists name='{}'", r.name());
            throw new NameAlreadyExistsException(r.name());
        }
        Release entity = mapper.toEntity(r);

        if (entity.getStatus() == ReleaseStatus.DONE && entity.getReleaseDate() == null) {
            entity.setReleaseDate(LocalDate.now());
        }

        Release saved = repo.save(entity);

        log.info("Release created id={} name='{}' status={}", saved.getId(), saved.getName(), saved.getStatus());
        log.debug("Create finished id={} durationMs={}", saved.getId(), (System.nanoTime() - t0) / 1_000_000);
        return mapper.toResponse(saved);
    }

    @Override
    public ReleaseResponse getById(UUID id) {
        long t0 = System.nanoTime();
        log.debug("GetById called id={}", id);
        Release release = repo.findById(id)
                .orElseThrow(() ->  new ResourceNotFoundException("Release with id " + id + " not found"));
        log.debug("GetById success id={} status={} durationMs={}", id, release.getStatus(), (System.nanoTime() - t0) / 1_000_000);
        return mapper.toResponse(release);
    }

    @Override
    public Page<ReleaseResponse> getAll(String statusLabel, String nameContains, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        long t0 = System.nanoTime();
        log.debug("List called filters={status:'{}', nameContains:'{}', dateFrom:{}, dateTo:{}} page={} size={}",
                statusLabel, nameContains, dateFrom, dateTo, pageable.getPageNumber(), pageable.getPageSize());

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
        log.debug("List finished items={} total={} durationMs={}",
                page.getNumberOfElements(), page.getTotalElements(), (System.nanoTime() - t0) / 1_000_000);
        return page.map(mapper::toResponse);
    }

    @Transactional
    @Override
    public ReleaseResponse update(UUID id, ReleaseRequest request) {
        long t0 = System.nanoTime();
        log.debug("Update called id={} name='{}' statusLabel='{}'", id, request.name(), request.status());

        Release entity = repo.findById(id)
                .orElseThrow(() ->  new ResourceNotFoundException("Release not found: " + id));

        ReleaseStatus oldStatus = entity.getStatus();

        if (!entity.getName().equals(request.name()) && repo.existsByName(request.name())) {
            log.warn("Update blocked: duplicate release name id={} newName='{}'", id, request.name());
            throw new NameAlreadyExistsException(request.name());
        }
        ReleaseStatus newStatus = ReleaseStatus.fromLabel(request.status());

        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setStatus(newStatus);

        LocalDate effectiveDate = request.releaseDate() != null ? request.releaseDate() : entity.getReleaseDate();
        if (newStatus == ReleaseStatus.DONE && effectiveDate == null) {
            effectiveDate = LocalDate.now();
        }
        entity.setReleaseDate(effectiveDate);

        entity.setLastUpdateAt(OffsetDateTime.now(ZoneOffset.UTC));

        Release saved = repo.save(entity);
        log.info("Release updated id={} name='{}' status:{}->{}", id, saved.getName(), oldStatus, saved.getStatus());
        log.debug("Update finished id={} durationMs={}", id, (System.nanoTime() - t0) / 1_000_000);
        return mapper.toResponse(saved);
    }

    @Transactional()
    @Override
    public void delete(UUID id) {
        long t0 = System.nanoTime();
        log.debug("Delete called id={}", id);
        Release release = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found with id: " + id));
        repo.delete(release);
        log.info("Release deleted id={} name='{}'", id, release.getName());
        log.debug("Delete finished id={} durationMs={}", id, (System.nanoTime() - t0) / 1_000_000);
    }
}
