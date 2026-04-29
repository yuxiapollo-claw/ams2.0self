package com.company.ams.access;

public record PermissionRequestRow(
        Long id,
        String requestNo,
        String requestType,
        String permissionPath,
        String currentStatus,
        String requestReason,
        String createdAt) {}
