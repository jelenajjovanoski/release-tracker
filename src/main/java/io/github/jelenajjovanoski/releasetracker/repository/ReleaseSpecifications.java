package io.github.jelenajjovanoski.releasetracker.repository;

import io.github.jelenajjovanoski.releasetracker.model.Release;
import io.github.jelenajjovanoski.releasetracker.model.ReleaseStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class ReleaseSpecifications {

    private ReleaseSpecifications() {}

    public static Specification<Release> hasStatus(ReleaseStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Release> nameContains(String term) {
        return (root, q, cb) -> (term == null || term.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + term.toLowerCase() + "%");
    }

    public static Specification<Release> releaseDateFrom(LocalDate from) {
        return (root, q, cb) -> from == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("releaseDate"), from);
    }

    public static Specification<Release> releaseDateTo(LocalDate to) {
        return (root, q, cb) -> to == null ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("releaseDate"), to);
    }
}
