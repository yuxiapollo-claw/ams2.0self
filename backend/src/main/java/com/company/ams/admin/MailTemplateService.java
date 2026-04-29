package com.company.ams.admin;

import com.company.ams.common.persistence.MailTemplateRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MailTemplateService {
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final int TEMPLATE_NAME_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 255;
    private static final int SUBJECT_MAX_LENGTH = 255;
    private static final int BODY_MAX_LENGTH = 4000;

    private final MailTemplateRepository mailTemplateRepository;

    public MailTemplateService(MailTemplateRepository mailTemplateRepository) {
        this.mailTemplateRepository = mailTemplateRepository;
    }

    public List<MailTemplateRow> list() {
        return mailTemplateRepository.findAll();
    }

    public MailTemplateRow create(MailTemplateUpsertCommand command) {
        validateUpsert(command);
        return mailTemplateRepository.create(command);
    }

    public MailTemplateRow update(long templateId, MailTemplateUpsertCommand command) {
        validateUpsert(command);
        return mailTemplateRepository.update(templateId, command);
    }

    public void delete(long templateId) {
        mailTemplateRepository.delete(templateId);
    }

    private void validateUpsert(MailTemplateUpsertCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.templateName() == null || command.templateName().isBlank()) {
            throw new IllegalArgumentException("templateName is required");
        }
        if (command.templateName().length() > TEMPLATE_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("templateName must be at most 100 characters");
        }
        if (command.subject() == null || command.subject().isBlank()) {
            throw new IllegalArgumentException("subject is required");
        }
        if (command.subject().length() > SUBJECT_MAX_LENGTH) {
            throw new IllegalArgumentException("subject must be at most 255 characters");
        }
        if (command.body() == null || command.body().isBlank()) {
            throw new IllegalArgumentException("body is required");
        }
        if (command.body().length() > BODY_MAX_LENGTH) {
            throw new IllegalArgumentException("body must be at most 4000 characters");
        }
        if (command.status() == null || command.status().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        validateStatus(command.status());
        if (command.description() != null && command.description().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("description must be at most 255 characters");
        }
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new IllegalArgumentException("status must be ENABLED or DISABLED");
        }
    }
}
