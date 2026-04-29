package com.company.ams.request;

public record RequestListItem(
        Long id,
        String requestNo,
        String requestType,
        String targetAccountName,
        String currentStatus,
        String reason,
        String createdAt) {}
