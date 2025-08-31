package io.github.jelenajjovanoski.releasetracker.service;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;
import io.github.jelenajjovanoski.releasetracker.exception.InvalidStatusException;
import io.github.jelenajjovanoski.releasetracker.exception.NameAlreadyExistsException;
import io.github.jelenajjovanoski.releasetracker.mapper.ReleaseMapper;
import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import io.github.jelenajjovanoski.releasetracker.repository.ReleaseRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ReleaseServicePutTest {

    @Mock
    ReleaseRepository repo;
    @Mock
    ReleaseMapper mapper;
    @InjectMocks
    ReleaseServiceImpl service;

    private static final String OLD_RELEASE_NAME = "Rel A";
    private static final String NEW_RELEASE_NAME = "Rel B";
    private static final String OLD_DESC = "Old";
    private static final String NEW_DESC = "New Desc";
    private static final ReleaseStatus OLD_STATUS = ReleaseStatus.CREATED;
    private static final ReleaseStatus NEW_STATUS = ReleaseStatus.ON_PROD;


    @Test
    void testUpdate_withValidRequest() {
        UUID id = UUID.randomUUID();
        LocalDate releaseDate = LocalDate.now().plusDays(1);

        Release existing = new Release();
        existing.setId(id);
        existing.setName(OLD_RELEASE_NAME);
        existing.setDescription(OLD_DESC);
        existing.setStatus(OLD_STATUS);
        existing.setReleaseDate(releaseDate);
        existing.setLastUpdateAt(OffsetDateTime.now());

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(Release.class))).thenAnswer(inv -> inv.getArgument(0));

        ReleaseResponse expected = new ReleaseResponse(id, OLD_RELEASE_NAME, NEW_DESC, NEW_STATUS.getLabel(),
                existing.getReleaseDate(), null, null
        );
        when(mapper.toResponse(org.mockito.ArgumentMatchers.nullable(Release.class)))
                .thenReturn(expected);

        ReleaseRequest updateRequest = new ReleaseRequest(
                OLD_RELEASE_NAME, NEW_DESC, NEW_STATUS.getLabel(), existing.getReleaseDate()
        );

        ReleaseResponse result = service.update(id, updateRequest);

        assertNotNull(result);
        assertEquals(OLD_RELEASE_NAME, result.name());
        assertEquals(NEW_DESC, result.description());
        assertEquals(NEW_STATUS.getLabel(), result.status());


        ArgumentCaptor<Release> captor = ArgumentCaptor.forClass(Release.class);
        verify(repo).save(captor.capture());
        Release saved = captor.getValue();
        assertEquals(OLD_RELEASE_NAME, saved.getName());
        assertEquals(NEW_DESC, saved.getDescription());
        assertEquals(NEW_STATUS, saved.getStatus());
        assertNotNull(saved.getLastUpdateAt());
    }

    @Test
    void testUpdate_withDuplicateName() {
        UUID id = UUID.randomUUID();

        Release existing = new Release();
        existing.setId(id);
        existing.setName(OLD_RELEASE_NAME);
        existing.setDescription(OLD_DESC);
        existing.setStatus(OLD_STATUS);
        existing.setReleaseDate(LocalDate.now().plusDays(1));

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        when(repo.existsByName(NEW_RELEASE_NAME)).thenReturn(true);

        ReleaseRequest updateRequest = new ReleaseRequest(NEW_RELEASE_NAME, NEW_DESC, OLD_STATUS.getLabel(),
                LocalDate.now().plusDays(2));

        assertThrows(NameAlreadyExistsException.class, () -> service.update(id, updateRequest));
        verify(repo, never()).save(any());
    }

    @Test
    void testUpdate_withInvalidStatus() {
        UUID id = UUID.randomUUID();

        Release existing = new Release();
        existing.setId(id);
        existing.setName(OLD_RELEASE_NAME);
        existing.setDescription(OLD_DESC);
        existing.setStatus(OLD_STATUS);
        existing.setReleaseDate(LocalDate.now().plusDays(1));

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        ReleaseRequest updateRequest = new ReleaseRequest("Rel A", "Desc",
                "INVALID_STATUS", LocalDate.now().plusDays(1));

        assertThrows(InvalidStatusException.class, () -> service.update(id, updateRequest));
        verify(repo, never()).save(any());
    }

}
