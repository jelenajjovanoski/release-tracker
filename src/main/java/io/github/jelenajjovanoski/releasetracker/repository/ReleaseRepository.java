package io.github.jelenajjovanoski.releasetracker.repository;

import io.github.jelenajjovanoski.releasetracker.model.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {

    boolean existsByName(String name);
}
