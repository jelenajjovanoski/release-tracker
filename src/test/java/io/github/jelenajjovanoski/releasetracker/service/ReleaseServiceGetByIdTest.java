package io.github.jelenajjovanoski.releasetracker.service;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;
import io.github.jelenajjovanoski.releasetracker.exception.ResourceNotFoundException;
import io.github.jelenajjovanoski.releasetracker.mapper.ReleaseMapper;
import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import io.github.jelenajjovanoski.releasetracker.repository.ReleaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReleaseServiceGetByIdTest {

    @Mock
    ReleaseRepository repo;
    @Mock
    ReleaseMapper mapper;
    @InjectMocks
    ReleaseServiceImpl service;

    @Test
    void testGetById_whenExists() {
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
    void testGetById_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
    }
}
