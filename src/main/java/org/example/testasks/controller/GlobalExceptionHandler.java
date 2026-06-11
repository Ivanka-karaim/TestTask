package org.example.testasks.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.testasks.api.dto.ErrorResponseDto;
import org.example.testasks.exception.BadRequestException;
import org.example.testasks.exception.ConflictException;
import org.example.testasks.exception.NotFoundException;
import org.example.testasks.external.ExternalServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
        log.error("NotFoundException", ex);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto("Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex) {
        log.error("BadRequestException", ex);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto("Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponseDto);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex) {
        log.error("ConflictException", ex);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto("Conflict", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponseDto);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("MethodArgumentNotValidException", ex);
        Map<String, String> body = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> body.put(err.getField(), err.getDefaultMessage()));
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Object> handleExternalService(ExternalServiceException ex) {
        log.error("External service error: status={}", ex.getStatus(), ex);
        ErrorResponseDto dto = new ErrorResponseDto("External Service Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(dto);
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("ConstraintViolationException", ex);
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(v -> {
            String field = v.getPropertyPath().toString();
            String message = v.getMessage();
            errors.put(field, message);
        });
        ErrorResponseDto response = new ErrorResponseDto(
                "Bad Request",
                errors.toString()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto("Internal Server Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDto);
    }
}
