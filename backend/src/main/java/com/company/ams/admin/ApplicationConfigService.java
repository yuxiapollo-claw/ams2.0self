package com.company.ams.admin;

import com.company.ams.common.persistence.ApplicationConfigRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationConfigService {
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final int APPLICATION_NAME_MAX_LENGTH = 100;
    private static final int APPLICATION_CODE_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final ApplicationConfigRepository applicationConfigRepository;

    public ApplicationConfigService(ApplicationConfigRepository applicationConfigRepository) {
        this.applicationConfigRepository = applicationConfigRepository;
    }

    public List<ApplicationConfigRow> list() {
        return applicationConfigRepository.findAll();
    }

    public ApplicationConfigRow create(ApplicationConfigUpsertCommand command) {
        validateUpsert(command);
        return applicationConfigRepository.create(command);
    }

    public ApplicationConfigRow update(long configId, ApplicationConfigUpsertCommand command) {
        validateUpsert(command);
        return applicationConfigRepository.update(configId, command);
    }

    public void delete(long configId) {
        applicationConfigRepository.delete(configId);
    }

    private void validateUpsert(ApplicationConfigUpsertCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.applicationName() == null || command.applicationName().isBlank()) {
            throw new IllegalArgumentException("applicationName is required");
        }
        if (command.applicationName().length() > APPLICATION_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("applicationName must be at most 100 characters");
        }
        if (command.applicationCode() == null || command.applicationCode().isBlank()) {
            throw new IllegalArgumentException("applicationCode is required");
        }
        if (command.applicationCode().length() > APPLICATION_CODE_MAX_LENGTH) {
            throw new IllegalArgumentException("applicationCode must be at most 100 characters");
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
