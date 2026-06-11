package org.example.testasks.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.testasks.api.dto.ErrorResponseDto;
import org.example.testasks.exception.BadRequestException;
import org.example.testasks.exception.ConflictException;
import org.example.testasks.exception.NotFoundException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class ExternalErrorHandler {

    private final ObjectMapper objectMapper;

    public void handle(RestClientResponseException ex) {

        ErrorResponseDto error = parseError(ex.getResponseBodyAsString());

        HttpStatusCode status = ex.getStatusCode();

        throw switch (status.value()) {
            case 400 -> new BadRequestException(error.getMessage());
            case 404 -> new NotFoundException(error.getMessage());
            case 409 -> new ConflictException(error.getMessage());
            default -> new ExternalServiceException(status, error.getMessage());
        };
    }

    private ErrorResponseDto parseError(String body) {
        try {
            return objectMapper.readValue(body, ErrorResponseDto.class);
        } catch (Exception e) {
            return new ErrorResponseDto("UNKNOWN", body);
        }
    }
}
