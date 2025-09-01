package io.github.jelenajjovanoski.releasetracker.exception;

import io.github.jelenajjovanoski.releasetracker.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                fieldErrors.put(err.getField(), err.getDefaultMessage())
        );
        return build(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid", fieldErrors);
    }

    @ExceptionHandler(NameAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleNameExists(NameAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStatus(InvalidStatusException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid status", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter", "Invalid UUID format: " + ex.getValue()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidFormat(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof DateTimeParseException || ex.getMessage().contains("LocalDate")) {
            return build(HttpStatus.BAD_REQUEST,
                    "Invalid date format",
                    "Release date must be in format yyyy-MM-dd");
        }
        return build(HttpStatus.BAD_REQUEST,
                "Malformed request",
                "Request body is not valid JSON or has invalid fields");
    }


    // Helpers
    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, String message) {
       return build(status, error, message, null);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, String message, Map<String, String> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                error,
                message,
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
