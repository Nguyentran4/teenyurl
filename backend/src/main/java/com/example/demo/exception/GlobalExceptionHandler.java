package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAliasAlreadyExists(
        AliasAlreadyExistsException exception,
        HttpServletRequest request
    ) {
        logHandledException(HttpStatus.CONFLICT, exception, request);
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUrl(InvalidUrlException exception, HttpServletRequest request) {
        logHandledException(HttpStatus.BAD_REQUEST, exception, request);
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UrlNotFoundException exception, HttpServletRequest request) {
        logHandledException(HttpStatus.NOT_FOUND, exception, request);
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpired(UrlExpiredException exception, HttpServletRequest request) {
        logHandledException(HttpStatus.GONE, exception, request);
        return error(HttpStatus.GONE, exception.getMessage(), request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
        RateLimitExceededException exception,
        HttpServletRequest request
    ) {
        logHandledException(HttpStatus.TOO_MANY_REQUESTS, exception, request);
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfter().toSeconds()))
            .body(errorBody(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        String message = exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        logHandledException(HttpStatus.BAD_REQUEST, exception, request);
        return error(HttpStatus.BAD_REQUEST, message.isBlank() ? "Request validation failed" : message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        String message = exception
            .getConstraintViolations()
            .stream()
            .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
            .collect(Collectors.joining("; "));

        logHandledException(HttpStatus.BAD_REQUEST, exception, request);
        return error(HttpStatus.BAD_REQUEST, message.isBlank() ? "Request validation failed" : message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        logHandledException(HttpStatus.BAD_REQUEST, exception, request);
        return error(HttpStatus.BAD_REQUEST, "Request body is malformed or contains invalid field values", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error(
            "Unhandled exception status={} method={} path={} message={}",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getMethod(),
            request.getRequestURI(),
            exception.getMessage(),
            exception
        );
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity
            .status(status)
            .body(errorBody(status, message, request));
    }

    private ErrorResponse errorBody(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI()
        );
    }

    private void logHandledException(HttpStatus status, Exception exception, HttpServletRequest request) {
        LOGGER.warn(
            "Handled exception status={} error={} method={} path={} message={}",
            status.value(),
            status.getReasonPhrase(),
            request.getMethod(),
            request.getRequestURI(),
            exception.getMessage()
        );
    }
}
