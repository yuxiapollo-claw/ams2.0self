package com.company.ams.common.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(4000, exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException exception) {
        String message = exception.getMessage() != null && exception.getMessage().contains("Required request body is missing")
                ? "Request body is required"
                : "Malformed JSON request";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(4000, message));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(BusinessException.DEFAULT_CODE, friendlyDuplicateMessage(exception)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String details = rootCauseMessage(exception);
        if (containsIgnoreCase(details, "fk_sys_user_department")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(4000, "departmentId does not exist"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(BusinessException.DEFAULT_CODE, friendlyDuplicateMessage(exception)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(4030, exception.getMessage() == null ? "Forbidden" : exception.getMessage()));
    }

    private String friendlyDuplicateMessage(RuntimeException exception) {
        String details = rootCauseMessage(exception);
        if (containsIgnoreCase(details, "uk_sys_user_code") || containsIgnoreCase(details, "sys_user(user_code")) {
            return "User code already exists";
        }
        if (containsIgnoreCase(details, "uk_sys_user_login") || containsIgnoreCase(details, "sys_user(login_name")) {
            return "Login name already exists";
        }
        if (containsIgnoreCase(details, "uk_access_system_name") || containsIgnoreCase(details, "access_system(system_name)")) {
            return "System name already exists";
        }
        if (containsIgnoreCase(details, "uk_admin_application_config_code")
                || containsIgnoreCase(details, "admin_application_config(application_code)")) {
            return "Application code already exists";
        }
        if (containsIgnoreCase(details, "uk_admin_mail_template_name")
                || containsIgnoreCase(details, "admin_mail_template(template_name)")) {
            return "Template name already exists";
        }
        return "Duplicate key";
    }

    private String rootCauseMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null) {
            return false;
        }
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }
}
