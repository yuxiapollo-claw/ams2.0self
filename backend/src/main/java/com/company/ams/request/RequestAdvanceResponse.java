package com.company.ams.request;

public record RequestAdvanceResponse(
        long requestId,
        String currentStatus,
        String currentStatusLabel) {}
