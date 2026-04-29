package com.company.ams.admin;

public record MailTemplateUpsertCommand(
        String templateName,
        String description,
        String subject,
        String body,
        String status) {}
