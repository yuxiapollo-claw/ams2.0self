package com.company.ams.admin;

public record ApplicationConfigUpsertCommand(
        String applicationName,
        String applicationCode,
        String description,
        String status) {}
