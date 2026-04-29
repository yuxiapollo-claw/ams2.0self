package com.company.ams.access;

public record PermissionRequestCommand(String requestType, Long permissionId, String reason) {}
