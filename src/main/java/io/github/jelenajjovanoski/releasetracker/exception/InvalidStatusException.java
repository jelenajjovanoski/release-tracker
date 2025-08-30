package io.github.jelenajjovanoski.releasetracker.exception;

public class InvalidStatusException extends RuntimeException {
    public InvalidStatusException(String status) {
        super("Unknown status: " + status);
    }
}
