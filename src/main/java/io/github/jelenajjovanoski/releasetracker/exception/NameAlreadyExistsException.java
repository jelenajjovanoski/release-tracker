package io.github.jelenajjovanoski.releasetracker.exception;

public class NameAlreadyExistsException extends RuntimeException {
    public NameAlreadyExistsException(String name) {
        super("Release name already exists: " + name);
    }
}
