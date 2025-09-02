package io.github.jelenajjovanoski.releasetracker.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, UUID>, JpaSpecificationExecutor<Release> {

    boolean existsByName(String name);
    List<Release> findByStatus(ReleaseStatus status);
}
