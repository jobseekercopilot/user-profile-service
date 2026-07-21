package com.jobseekercopilot.userprofileservice.exception;

import com.jobseekercopilot.userprofileservice.logging.CorrelationIdFilter;
import com.jobseekercopilot.userprofileservice.web.PayloadTooLargeIOException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::safeViolation)
                .distinct()
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::code))
                .toList();
        return validation(violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(lastPathSegment(violation.getPropertyPath().toString()),
                        violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
                                .toUpperCase(Locale.ROOT)))
                .distinct()
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::code))
                .toList();
        return validation(violations);
    }

    @ExceptionHandler(ProfileValidationException.class)
    ResponseEntity<ErrorResponse> handleProfileValidation(ProfileValidationException exception) {
        return validation(List.of(new FieldViolation(exception.getField(), exception.getCode())));
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> handleBadRequest() {
        return failure(HttpStatus.BAD_REQUEST, "PROFILE_VALIDATION_FAILED", "Profile validation failed.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleResourceNotFound() {
        return failure(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "User profile was not found.");
    }

    @ExceptionHandler(ProfileWriteConflictException.class)
    ResponseEntity<ErrorResponse> handleProfileWriteConflict() {
        return failure(HttpStatus.CONFLICT, "PROFILE_WRITE_CONFLICT",
                "The profile changed concurrently; retry the request.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException exception) {
        if (hasCause(exception, PayloadTooLargeIOException.class)) {
            return failure(HttpStatus.CONTENT_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Request body is too large.");
        }
        return failure(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Request body is not valid JSON.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleUnsupportedMediaType() {
        return failure(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type must be application/json.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFoundResource() {
        return failure(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled profile request failure error={}", exception.getClass().getSimpleName());
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> validation(List<FieldViolation> violations) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "PROFILE_VALIDATION_FAILED", "Profile validation failed.", correlationId(), violations));
    }

    private ResponseEntity<ErrorResponse> failure(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, correlationId()));
    }

    private FieldViolation safeViolation(FieldError error) {
        String code = error.getCode() == null ? "INVALID" : error.getCode().toUpperCase(Locale.ROOT);
        return new FieldViolation(error.getField().replace(".<list element>", ""), code);
    }

    private String correlationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }

    private String lastPathSegment(String path) {
        int separator = path.lastIndexOf('.');
        return separator < 0 ? path : path.substring(separator + 1);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
