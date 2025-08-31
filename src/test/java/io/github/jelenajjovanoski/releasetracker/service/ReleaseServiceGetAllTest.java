package io.github.jelenajjovanoski.releasetracker.service;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;
import io.github.jelenajjovanoski.releasetracker.exception.InvalidStatusException;
import io.github.jelenajjovanoski.releasetracker.mapper.ReleaseMapper;
import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import io.github.jelenajjovanoski.releasetracker.repository.ReleaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ReleaseServiceGetAllTest {
    @Mock
    ReleaseRepository repo;
    @Mock
    ReleaseMapper mapper;
    @InjectMocks
    ReleaseServiceImpl service;

    Release e1;
    ReleaseResponse r1;

    private static final String RELEASE_NAME = "Release 1";
    private static final String RELEASE_STATUS = "Created";

    @BeforeEach
    void setUp() {
        e1 = new Release();
        e1.setId(UUID.randomUUID());
        e1.setName(RELEASE_NAME);
        e1.setDescription("Description");
        e1.setStatus(ReleaseStatus.fromLabel(RELEASE_STATUS));
        e1.setReleaseDate(LocalDate.of(2025, 9, 1));
        e1.setCreatedAt(OffsetDateTime.now().minusDays(1));
        e1.setLastUpdateAt(OffsetDateTime.now());

        r1 = new ReleaseResponse(
                e1.getId(),
                e1.getName(),
                e1.getDescription(),
                e1.getStatus().getLabel(),
                e1.getReleaseDate(),
                e1.getCreatedAt(),
                e1.getLastUpdateAt()
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withoutFilters() {
        Pageable reqPageable = PageRequest.of(0, 20);
        Page<Release> repoPage = new PageImpl<>(List.of(e1), reqPageable, 1);

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(repoPage);
        when(mapper.toResponse(e1)).thenReturn(r1);

        Page<ReleaseResponse> result = service.getAll(null, null, null, null, reqPageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(RELEASE_NAME, result.getContent().get(0).name());

    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withStatusLabel_parsesStatusAndCallsRepo() {

        Pageable reqPageable = PageRequest.of(0, 10);
        Page<Release> repoPage = new PageImpl<>(List.of(e1), reqPageable, 1);
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(repoPage);
        when(mapper.toResponse(e1)).thenReturn(r1);

        Page<ReleaseResponse> result = service.getAll(RELEASE_STATUS, null, null, null, reqPageable);

        assertEquals(1, result.getTotalElements());
        verify(repo, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(mapper).toResponse(e1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withInvalidStatusLabel_throwsAndDoesNotCallRepo() {

        Pageable reqPageable = PageRequest.of(0, 20);

        assertThrows(InvalidStatusException.class,
                () -> service.getAll("On MARS", null, null, null, reqPageable));

        verifyNoInteractions(repo, mapper);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAll_withNameAndDateRange_filtersAndMaps() {

        Pageable reqPageable = PageRequest.of(0, 10);
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1), reqPageable, 1));

        when(mapper.toResponse(e1)).thenReturn(r1);

        Page<ReleaseResponse> result = service.getAll(
                null, "Rel",
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 30),
                reqPageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getNumberOfElements());
        verify(mapper, atLeastOnce()).toResponse(any(Release.class));
    }
}
