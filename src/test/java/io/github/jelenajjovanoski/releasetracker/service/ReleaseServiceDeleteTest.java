package io.github.jelenajjovanoski.releasetracker.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.repository.ReleaseRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReleaseServiceDeleteTest {

    @Mock
    ReleaseRepository repo;

    @InjectMocks
    ReleaseServiceImpl service;

    @Test
    void testDelete_withExistingId() {
        UUID id = UUID.randomUUID();
        Release release = new Release();
        release.setId(id);

        when(repo.findById(id)).thenReturn(Optional.of(release));

        service.delete(id);

        verify(repo).delete(release);
    }
}
