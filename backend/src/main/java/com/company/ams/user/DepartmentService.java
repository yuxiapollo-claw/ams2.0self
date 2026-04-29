package com.company.ams.user;

import com.company.ams.common.api.BusinessException;
import com.company.ams.common.persistence.DepartmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {
    private static final String DEPARTMENT_STATUS_ENABLED = "ENABLED";
    private static final String DEPARTMENT_STATUS_DISABLED = "DISABLED";
    private static final String DEPARTMENT_HAS_MEMBERS_MESSAGE = "Department still has members";
    private static final int DEPARTMENT_NAME_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentRow> list() {
        return departmentRepository.findAll();
    }

    public DepartmentRow create(DepartmentUpsertCommand command) {
        validateUpsert(command);
        return departmentRepository.create(command);
    }

    public DepartmentRow update(long departmentId, DepartmentUpsertCommand command) {
        validateUpsert(command);
        return departmentRepository.update(departmentId, command);
    }

    public void delete(long departmentId) {
        if (departmentRepository.existsNonDeletedMembers(departmentId)) {
            throw new BusinessException(DEPARTMENT_HAS_MEMBERS_MESSAGE);
        }
        departmentRepository.delete(departmentId);
    }

    private void validateUpsert(DepartmentUpsertCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.departmentName() == null || command.departmentName().isBlank()) {
            throw new IllegalArgumentException("departmentName is required");
        }
        if (command.departmentName().length() > DEPARTMENT_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("departmentName must be at most 100 characters");
        }
        if (command.status() == null || command.status().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        validateStatus(command.status());
        if (command.description() != null && command.description().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("description must be at most 255 characters");
        }
        if (command.managerUserId() != null && !departmentRepository.existsNonDeletedUser(command.managerUserId())) {
            throw new IllegalArgumentException("managerUserId does not exist");
        }
    }

    private void validateStatus(String status) {
        if (!DEPARTMENT_STATUS_ENABLED.equals(status) && !DEPARTMENT_STATUS_DISABLED.equals(status)) {
            throw new IllegalArgumentException("status must be ENABLED or DISABLED");
        }
    }
}
