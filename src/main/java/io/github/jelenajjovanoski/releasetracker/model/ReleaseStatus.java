package io.github.jelenajjovanoski.releasetracker.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.jelenajjovanoski.releasetracker.exception.InvalidStatusException;

public enum ReleaseStatus {
    CREATED("Created"),
    IN_DEVELOPMENT("In Development"),
    ON_DEV("On DEV"),
    QA_DONE_ON_DEV("QA Done on DEV"),
    ON_STAGING("On staging"),
    QA_DONE_ON_STAGING("QA done on STAGING"),
    ON_PROD("On PROD"),
    DONE("Done");

    private static final Logger log = LoggerFactory.getLogger(ReleaseStatus.class);
    private final String label;

    ReleaseStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ReleaseStatus fromLabel(String label) {
        for (ReleaseStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new InvalidStatusException(label);
    }
}
