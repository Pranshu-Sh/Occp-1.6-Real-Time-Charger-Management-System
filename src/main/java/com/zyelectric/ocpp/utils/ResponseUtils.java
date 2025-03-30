package com.zyelectric.ocpp.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtils {

    private ResponseUtils() {
    }
    public static ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message,
            Object data
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);

        if (data != null) {
            response.put("data", data);
        }

        return new ResponseEntity<>(response, status);
    }
}
