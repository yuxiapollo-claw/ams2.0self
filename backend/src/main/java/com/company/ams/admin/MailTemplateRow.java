package com.company.ams.admin;

public record MailTemplateRow(
        long id,
        String templateName,
        String description,
        String subject,
        String body,
        String status,
        String updatedAt) {}
