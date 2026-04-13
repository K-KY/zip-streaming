package com.example.download.exception;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DirectoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDirectoryNotFound(
            DirectoryNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("Directory not found for request path={}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ZipStreamingException.class)
    public ResponseEntity<ErrorResponse> handleZipStreamingException(
            ZipStreamingException exception,
            HttpServletRequest request
    ) {
        log.error("ZIP streaming exception for path={}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception for path={}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal server error",
                        request.getRequestURI()
                ));
    }

    public record ErrorResponse(
            Instant timestamp,
            int status,
            String message,
            String path
    ) {
        public static ErrorResponse of(int status, String message, String path) {
            return new ErrorResponse(Instant.now(), status, message, path);
        }
    }
}
