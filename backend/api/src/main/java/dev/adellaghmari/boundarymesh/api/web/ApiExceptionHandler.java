package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.domain.exception.DomainException;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(InvalidInputException.class)
    ResponseEntity<ApiError> invalidInput(InvalidInputException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.errorCode(), ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.errorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        return error(HttpStatus.BAD_REQUEST, "REQUEST_INVALID", "Request body failed validation.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "REQUEST_INVALID", "Request body was not valid JSON.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> notFound(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMedia(HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "The content type is not accepted.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        log.error("Unhandled exception request_id={}", MDC.get(CorrelationIdFilter.MDC_KEY), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
        ApiError body = new ApiError(clock.instant(), requestId == null ? "unknown" : requestId, code, message);
        return ResponseEntity.status(status).body(body);
    }

    public record ApiError(
            Instant timestamp,
            @JsonProperty("request_id") String requestId,
            @JsonProperty("error_code") String errorCode,
            String message) {}
}
