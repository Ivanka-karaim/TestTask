package org.example.testasks.api.dto;

public record ErrorResponseDto(
        String error,
        String message
) {}
