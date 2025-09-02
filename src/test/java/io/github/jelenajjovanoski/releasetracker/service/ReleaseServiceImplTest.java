package io.github.jelenajjovanoski.releasetracker.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.jelenajjovanoski.releasetracker.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.jelenajjovanoski.releasetracker.dto.ReleaseRequest;
import io.github.jelenajjovanoski.releasetracker.dto.ReleaseResponse;
import io.github.jelenajjovanoski.releasetracker.exception.InvalidStatusException;
import io.github.jelenajjovanoski.releasetracker.exception.NameAlreadyExistsException;
import io.github.jelenajjovanoski.releasetracker.mapper.ReleaseMapper;
import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import io.github.jelenajjovanoski.releasetracker.repository.ReleaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReleaseServiceImplTest {
    @Mock
    ReleaseRepository repo;
    @Mock
    ReleaseMapper mapper;
    @InjectMocks
    ReleaseServiceImpl service;

    @Nested
    class Create {

        @Test
        void create_whenValidRequest_shouldPersistAndReturnResponse() {
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
        public void create_whenDuplicateName_shouldThrowNameAlreadyExists() {
            ReleaseRequest req = new ReleaseRequest(
                    "Release 1", "Description", "Created", LocalDate.now());

            when(repo.existsByName("Release 1")).thenReturn(true);

            assertThrows(NameAlreadyExistsException.class, () -> service.create(req));
            verify(repo, never()).save(any());
        }

        @Test
        void create_whenInvalidStatus_shouldThrowInvalidStatusException() {
            ReleaseRequest req = new ReleaseRequest(
                    "Invalid status release", "Some description", "On MARS", LocalDate.now());

            when(repo.existsByName("Invalid status release")).thenReturn(false);
            when(mapper.toEntity(req)).thenThrow(new InvalidStatusException("On MARS"));

            assertThrows(InvalidStatusException.class, () -> service.create(req));

            verify(repo).existsByName("Invalid status release");
            verify(repo, never()).save(any());
            verify(mapper, never()).toResponse(any());
        }
    }

    @Nested
    class GetById {
        @Test
        void getById_whenExisting_shouldReturnResponse() {
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
        void getById_whenNonExisting_shouldThrowNotFound() {
            UUID id = UUID.randomUUID();
            when(repo.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
        }
    }

    @Nested
    class GetAll {

        private static final String RELEASE_NAME = "Release 1";
        private static final String RELEASE_STATUS = "Created";

        Release e1;
        ReleaseResponse r1;

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
        void getAll_whenNoFilters_shouldReturnPage() {
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
        void getAll_whenValidStatus_shouldReturnMatchingPage() {

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
        void getAll_whenInvalidStatus_shouldThrowInvalidStatus() {

            Pageable reqPageable = PageRequest.of(0, 20);

            assertThrows(InvalidStatusException.class,
                    () -> service.getAll("On MARS", null, null, null, reqPageable));

            verifyNoInteractions(repo, mapper);
        }

        @SuppressWarnings("unchecked")
        @Test
        void getAll_whenNameAndDateRange_shouldReturnFilteredPage() {

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

    @Nested
    class Update {
        private static final String OLD_RELEASE_NAME = "Rel A";
        private static final String NEW_RELEASE_NAME = "Rel B";
        private static final String OLD_DESC = "Old";
        private static final String NEW_DESC = "New Desc";
        private static final ReleaseStatus OLD_STATUS = ReleaseStatus.CREATED;
        private static final ReleaseStatus NEW_STATUS = ReleaseStatus.ON_PROD;

        @Test
        void update_whenValidRequest_shouldUpdateFieldsAndReturnResponse() {
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
        void update_whenDuplicateName_shouldThrowNameAlreadyExists() {
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
        void update_whenInvalidStatus_shouldThrowInvalidStatus() {
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

    @Nested
    class Delete {
        @Test
        void delete_whenExisting_shouldDelete() {
            UUID id = UUID.randomUUID();
            Release release = new Release();
            release.setId(id);

            when(repo.findById(id)).thenReturn(Optional.of(release));

            service.delete(id);

            verify(repo).delete(release);
        }
    }

}
