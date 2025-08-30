package io.github.jelenajjovanoski.releasetracker.service;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;
import io.github.jelenajjovanoski.releasetracker.exception.InvalidStatusException;
import io.github.jelenajjovanoski.releasetracker.exception.NameAlreadyExistsException;
import io.github.jelenajjovanoski.releasetracker.exception.ResourceNotFoundException;
import io.github.jelenajjovanoski.releasetracker.mapper.ReleaseMapper;
import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import io.github.jelenajjovanoski.releasetracker.repository.ReleaseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


@ExtendWith(MockitoExtension.class)
public class ReleaseServiceTest {
    @Mock
    ReleaseRepository repo;
    @Mock
    ReleaseMapper mapper;
    @InjectMocks
    ReleaseServiceImpl service;

    @Test
    public void create_withExistingName_throwsException() {
        ReleaseRequest req = new ReleaseRequest(
                "Release 1", "Description", "Created", LocalDate.now());

        when(repo.existsByName("Release 1")).thenReturn(true);

        assertThrows(NameAlreadyExistsException.class, () -> service.create(req));
        verify(repo, never()).save(any());
    }

    @Test
    void create_withValidRequest_returnsSavedResponse() {
        final String name = "Release 1";
        final String desc = "Some desc";
        final String status = "Created";
        final UUID id = UUID.randomUUID();
        final LocalDate releaseDate = LocalDate.now();

        ReleaseRequest req = new ReleaseRequest(name, desc, status, releaseDate);

        Release toPersist = new Release();
        toPersist.setName(name);
        toPersist.setDescription(desc);
        toPersist.setStatus(ReleaseStatus.CREATED);

        Release persisted = new Release();
        persisted.setId(id);
        persisted.setName(name);
        persisted.setDescription(desc);
        persisted.setStatus(ReleaseStatus.CREATED);

        when(repo.existsByName(name)).thenReturn(false);
        when(mapper.toEntity(req)).thenReturn(toPersist);
        when(repo.save(toPersist)).thenReturn(persisted);

        ReleaseResponse expected = new ReleaseResponse(
                id, name, desc, status, null, null, null);

        when(mapper.toResponse(persisted)).thenReturn(expected);
        ReleaseResponse result = service.create(req);

        assertNotNull(result);
        assertEquals(id, result.id());
        assertEquals(name, result.name());
        verify(repo).existsByName(name);
        verify(mapper).toEntity(req);
        verify(repo).save(toPersist);
        verify(mapper).toResponse(persisted);
    }

    @Test
    void create_withInvalidStatus_throwsException() {
        ReleaseRequest req = new ReleaseRequest(
                "Invalid status release", "Some description", "On MARS", LocalDate.now());

        when(repo.existsByName("Invalid status release")).thenReturn(false);
        when(mapper.toEntity(req)).thenThrow(new InvalidStatusException("On MARS"));

        assertThrows(InvalidStatusException.class, () -> service.create(req));

        verify(repo).existsByName("Invalid status release");
        verify(repo, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void getById_whenExists_returnsResponse() {
        final String name = "Release 1";
        final String desc = "Some desc";
        final String status = "Created";
        final UUID id = UUID.randomUUID();

        Release toPersist = new Release();
        toPersist.setName(name);
        toPersist.setDescription(desc);
        toPersist.setStatus(ReleaseStatus.fromLabel(status));

        when(repo.findById(id)).thenReturn(Optional.of(toPersist));

        ReleaseResponse expected = new ReleaseResponse(
                id,
                toPersist.getName(),
                toPersist.getDescription(),
                toPersist.getStatus().getLabel(),
                toPersist.getReleaseDate(),
                toPersist.getCreatedAt(),
                toPersist.getLastUpdateAt()
        );

        when(mapper.toResponse(toPersist)).thenReturn(expected);
        ReleaseResponse result = service.getById(id);

        assertNotNull(result);
        assertEquals(id, result.id());
        assertEquals(name, result.name());
        verify(repo).findById(id);
        verify(mapper).toResponse(toPersist);
    }

    @Test
    void getById_shouldThrow404_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
    }
}
