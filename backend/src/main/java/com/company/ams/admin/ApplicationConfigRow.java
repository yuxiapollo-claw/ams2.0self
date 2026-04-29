package com.company.ams.admin;

public record ApplicationConfigRow(
        long id,
        String applicationName,
        String applicationCode,
        String description,
        String status,
        String updatedAt) {}
