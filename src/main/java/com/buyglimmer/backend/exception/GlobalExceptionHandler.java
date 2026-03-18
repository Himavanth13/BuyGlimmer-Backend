package com.buyglimmer.backend.exception;

import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.util.ApiResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        private final ApiResponseFactory apiResponseFactory;

        public GlobalExceptionHandler(ApiResponseFactory apiResponseFactory) {
                this.apiResponseFactory = apiResponseFactory;
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiWrapperResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
                List<String> details = exception.getBindingResult().getAllErrors().stream()
                                .map(error -> error instanceof FieldError fieldError
                                                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                                                : error.getDefaultMessage())
                                .toList();

                logger.warn("Request validation failed: {}", details);
                return ResponseEntity.badRequest().body(apiResponseFactory.failed(null, String.join("; ", details)));
        }

        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ApiWrapperResponse<Object>> handleApiException(ApiException exception) {
                logger.warn("API exception: {}", exception.getMessage());
                return ResponseEntity.status(exception.getStatus()).body(apiResponseFactory.failed(null, exception.getMessage()));
        }

        @ExceptionHandler(NoSuchElementException.class)
        public ResponseEntity<ApiWrapperResponse<Object>> handleNotFound(NoSuchElementException exception) {
                logger.warn("Resource not found: {}", exception.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseFactory.failed(null, exception.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiWrapperResponse<Object>> handleUnhandled(Exception exception) {
                logger.error("Unhandled exception", exception);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponseFactory.failed(null, "Internal server error"));
        }
}