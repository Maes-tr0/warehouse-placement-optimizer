package com.nikitaopara.warehouseoptimizer.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                resolveMessage(exception, "Request is invalid"),
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        log.debug("Data conflict while processing {}", request.getRequestURI(), exception);

        return buildResponse(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "The resource was changed or reserved by another request. Refresh and try again",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected error while processing {}", request.getRequestURI(), exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    private String resolveMessage(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }
}
