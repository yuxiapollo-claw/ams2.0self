package com.company.ams.audit;

public record AuditLogRow(
        Long id,
        String actionType,
        String operatorName,
        String objectType,
        String createdAt) {}
