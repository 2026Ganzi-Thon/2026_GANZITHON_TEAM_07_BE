package com.ganzithon.backend.auth.adapter.in.web;

public record ApiEnvelope<T>(boolean success, T data, ApiError error) {
    public static <T> ApiEnvelope<T> success(T data) {
        return new ApiEnvelope<>(true, data, null);
    }

    public static ApiEnvelope<Object> failure(String code, String message, Object data) {
        return new ApiEnvelope<>(false, data, new ApiError(code, message));
    }

    public record ApiError(String code, String message) {}
}
