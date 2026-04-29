package com.company.ams.common.api;

public record ApiResponse<T>(int code, String message, T data, String traceId) {
    private static final String DEFAULT_TRACE_ID = "local-dev";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, DEFAULT_TRACE_ID);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, DEFAULT_TRACE_ID);
    }
}
