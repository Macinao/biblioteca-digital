package com.digital.smartteca.gateway.treatment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiError {

    private final int statusCode;
    private final String status;
    private final String message;
    private final String details;
    private final String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private final LocalDateTime timestamp;

    public static ApiError of(HttpStatus status, String message, String details, String path) {
        return ApiError.builder()
                .statusCode(status.value())
                .status(status.name())
                .message(message)
                .details(details)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
