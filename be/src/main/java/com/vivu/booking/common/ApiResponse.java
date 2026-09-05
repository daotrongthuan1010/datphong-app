package com.vivu.booking.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private Map<String,String> error;
    private T data;
    private String requestId;
    private long timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true).message("OK").data(data)
                .timestamp(System.currentTimeMillis()).build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true).message(message).data(data)
                .timestamp(System.currentTimeMillis()).build();
    }

    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false).message(message)
                .timestamp(System.currentTimeMillis()).build();
    }
    public static <T> ApiResponse<T> fails(String message,Map<String,String> errors) {
        return ApiResponse.<T>builder()
                .success(false).message(message).error(errors)
                .timestamp(System.currentTimeMillis()).build();
    }
}
