package io.github.jelenajjovanoski.releasetracker.repository;

import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, UUID>, JpaSpecificationExecutor<Release> {

    boolean existsByName(String name);
    List<Release> findByStatus(ReleaseStatus status);
}
