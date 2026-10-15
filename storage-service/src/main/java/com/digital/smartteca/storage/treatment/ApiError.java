package com.digital.smartteca.storage.treatment;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

@Getter
public class ApiError {

    private final String message;
    private final String details;
    private final String path;
    private final String status;
    private final int statusCode;
    private final String timestamp;

    private ApiError(HttpStatus status, String message, String details, String path) {
        this.status = status.name();
        this.statusCode = status.value();
        this.message = message;
        this.details = details;
        this.path = path;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public static ApiError of(HttpStatus status, String message, String details, String path) {
        return new ApiError(status, message, details, path);
    }

}
