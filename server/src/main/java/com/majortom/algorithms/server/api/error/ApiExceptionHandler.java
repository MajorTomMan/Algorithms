package com.majortom.algorithms.server.api.error;

import com.majortom.algorithms.server.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public final class ApiExceptionHandler {

    @ExceptionHandler({AlgorithmNotFoundException.class, ExecutionNotFoundException.class})
    public ResponseEntity<ErrorResponse> notFound(RuntimeException failure, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "not_found", failure, request);
    }

    @ExceptionHandler(ExecutionRejectedException.class)
    public ResponseEntity<ErrorResponse> rejected(ExecutionRejectedException failure, HttpServletRequest request) {
        return response(HttpStatus.TOO_MANY_REQUESTS, "scheduler_queue_full", failure, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> invalidInput(IllegalArgumentException failure, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "invalid_algorithm_input", failure, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> internalFailure(Exception failure, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "internal_execution_failure", failure, request);
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status, String code, Throwable failure, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status.value(), code, message(failure), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private static String message(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }
}
